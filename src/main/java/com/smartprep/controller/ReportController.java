package com.smartprep.controller;

import com.smartprep.repository.UserRepository;
import com.smartprep.service.EmailReportService;
import com.smartprep.service.EmailReportService.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

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

    /**
     * Sends the weekly report for a student to both configured email addresses.
     *
     * @param userId the student's user id
     * @return 200 { message, sentTo } when both emails were sent;
     *         404 if the user doesn't exist;
     *         502 { message, sentTo, failedTo } if any email could not be sent
     */
    @PostMapping("/send/{userId}")
    public ResponseEntity<Map<String, Object>> sendReport(@PathVariable int userId) {
        Map<String, Object> body = new LinkedHashMap<>();

        if (!userRepository.existsById(userId)) {
            body.put("message", "User " + userId + " not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        try {
            SendResult result = emailReportService.sendWeeklyReport(userId);
            body.put("sentTo", result.sentTo());
            if (result.allSent()) {
                body.put("message", "Report sent successfully!");
                return ResponseEntity.ok(body);
            }
            body.put("message", result.sentTo().isEmpty()
                    ? "Report could not be sent. Check the mail settings and server logs."
                    : "Report sent to some recipients only.");
            body.put("failedTo", result.failedTo());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
        } catch (Exception e) {
            log.error("Failed to build weekly report for user {}", userId, e);
            body.put("message", "Report could not be generated. Please try again later.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }
}
