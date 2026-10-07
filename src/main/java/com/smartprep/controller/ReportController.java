package com.smartprep.controller;

import com.smartprep.repository.UserRepository;
import com.smartprep.service.EmailReportService;
import com.smartprep.service.EmailReportService.SendResult;
import com.smartprep.service.ScheduledReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Emails a student's weekly progress report to the parent and the student.
 */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private EmailReportService emailReportService;

    @Autowired
    private UserRepository userRepository;

    /** Shared secret for the external cron; the cron endpoint answers 404 while it is unset. */
    @Value("${CRON_TOKEN:}")
    private String cronToken;

    /** Minimum time between two manual sends for the same user. */
    private static final Duration SEND_COOLDOWN = Duration.ofMinutes(5);

    /** userId -> time of the last accepted manual send (in memory, reset on restart). */
    private final Map<Integer, Instant> lastManualSend = new ConcurrentHashMap<>();

    /**
     * Sends the weekly report for a student to both configured email addresses.
     *
     * @param userId the student's user id
     * @return 200 { message, sentTo } when both emails were sent;
     *         404 if the user doesn't exist;
     *         429 { message } if a report for this user was sent in the last 5 minutes;
     *         502 { message, sentTo, failedTo } if any email could not be sent
     */
    @PostMapping("/send/{userId}")
    public ResponseEntity<Map<String, Object>> sendReport(@PathVariable int userId) {
        Map<String, Object> body = new LinkedHashMap<>();

        if (!userRepository.existsById(userId)) {
            body.put("message", "User " + userId + " not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        Instant now = Instant.now();
        Instant previous = claimSendSlot(userId, now);
        if (previous != null) {
            long waitSeconds = Math.max(1, Duration.between(now, previous.plus(SEND_COOLDOWN)).toSeconds());
            body.put("message", "A report was sent recently. Please try again in a few minutes.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(waitSeconds))
                    .body(body);
        }

        try {
            SendResult result = emailReportService.sendWeeklyReport(userId);
            body.put("sentTo", result.sentTo());
            if (result.allSent()) {
                body.put("message", "Report sent successfully!");
                return ResponseEntity.ok(body);
            }
            if (result.sentTo().isEmpty()) {
                lastManualSend.remove(userId, now); // nothing went out, so allow a retry
            }
            body.put("message", result.sentTo().isEmpty()
                    ? "Report could not be sent. Check the mail settings and server logs."
                    : "Report sent to some recipients only.");
            body.put("failedTo", result.failedTo());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
        } catch (Exception e) {
            lastManualSend.remove(userId, now);
            log.error("Failed to build weekly report for user {}", userId, e);
            body.put("message", "Report could not be generated. Please try again later.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    /**
     * Sends the nightly report for the scheduled student; called by an external cron job.
     * Requires header X-Cron-Token equal to env CRON_TOKEN.
     *
     * @return 200 { message } when sent; 401 for a missing or wrong token; 404 if CRON_TOKEN is unset;
     *         502 { message } if any email could not be sent
     */
    @PostMapping("/cron/send")
    public ResponseEntity<Map<String, Object>> cronSend(
            @RequestHeader(value = "X-Cron-Token", required = false) String token) {
        if (cronToken.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        if (token == null || !MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), cronToken.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid cron token"));
        }

        int userId = ScheduledReportService.STUDENT_USER_ID;
        try {
            SendResult result = emailReportService.sendWeeklyReport(userId);
            if (result.allSent()) {
                log.info("Cron report for user {} sent to {} recipient(s)", userId, result.sentTo().size());
                return ResponseEntity.ok(Map.of("message",
                        "Report sent to " + result.sentTo().size() + " recipient(s)"));
            }
            log.error("Cron report for user {} failed for {} recipient(s) (sent to {})",
                    userId, result.failedTo().size(), result.sentTo().size());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message",
                    "Report could not be sent to every recipient. Check the server logs."));
        } catch (Exception e) {
            log.error("Failed to build cron report for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Report could not be generated."));
        }
    }

    /** Atomically records a send for this user unless one happened within the cooldown; returns that earlier time. */
    private Instant claimSendSlot(int userId, Instant now) {
        Instant[] blockedBy = new Instant[1];
        lastManualSend.compute(userId, (id, last) -> {
            if (last != null && now.isBefore(last.plus(SEND_COOLDOWN))) {
                blockedBy[0] = last;
                return last;
            }
            return now;
        });
        return blockedBy[0];
    }
}
