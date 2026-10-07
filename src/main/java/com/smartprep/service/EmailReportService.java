package com.smartprep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartprep.dto.DashboardDTO;
import com.smartprep.dto.WeakTopicDTO;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.HtmlUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Builds the daily SmartPrep progress report and emails it to the parent and the student.
 */
@Service
public class EmailReportService {

    private static final Logger log = LoggerFactory.getLogger(EmailReportService.class);

    private static final double WEAK_THRESHOLD = 60.0;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");
    private static final Duration BREVO_TIMEOUT = Duration.ofSeconds(15);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[^\\s@<>\"']+@[^\\s@<>\"']+");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final WebClient webClient = WebClient.create();

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private StudyLogService studyLogService;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    /** Comma-separated; every report goes to each address as its own email. No default on purpose. */
    @Value("${REPORT_RECIPIENTS:}")
    private String reportRecipients;

    /** When set, mail goes through Brevo's HTTPS API instead of SMTP (Render's free plan blocks SMTP). */
    @Value("${BREVO_API_KEY:}")
    private String brevoApiKey;

    @Value("${MAIL_FROM_EMAIL:${spring.mail.username:}}")
    private String brevoFromEmail;

    @Value("${MAIL_FROM_NAME:SmartPrep}")
    private String brevoFromName;

    @Value("${brevo.base-url:https://api.brevo.com}")
    private String brevoBaseUrl;

    /**
     * Result of sending one report: which recipients got it and which failed.
     *
     * @param sentTo   addresses the email was delivered to the mail server for
     * @param failedTo addresses that could not be sent to
     */
    public record SendResult(List<String> sentTo, List<String> failedTo) {
        /** @return true if the report was sent and every recipient got it */
        public boolean allSent() { return !sentTo.isEmpty() && failedTo.isEmpty(); }
    }

    /**
     * Fetches the student's dashboard and daily study hours, then emails the HTML report to
     * every address in REPORT_RECIPIENTS. A failure for one recipient does not stop the others.
     * Sends nothing (and logs a warning) if REPORT_RECIPIENTS is not set.
     *
     * @param userId the student's user id
     * @return which recipients were sent the report and which failed
     */
    public SendResult sendWeeklyReport(int userId) {
        List<String> recipients = getRecipients();
        if (recipients.isEmpty()) {
            log.warn("REPORT_RECIPIENTS is not set; report for user {} was not sent", userId);
            return new SendResult(List.of(), List.of());
        }

        DashboardDTO dashboard = dashboardService.getDashboard(userId);
        double weekHours = getWeekHours(userId);

        String subject = "SmartPrep Weekly Report – " + dashboard.getName();
        String html = buildHtml(dashboard, weekHours);
        String text = buildPlainText(dashboard, weekHours);

        List<String> sentTo = new ArrayList<>();
        List<String> failedTo = new ArrayList<>();
        for (int i = 0; i < recipients.size(); i++) {
            String recipient = recipients.get(i);
            try {
                if (brevoApiKey.isBlank()) {
                    sendViaSmtp(recipient, subject, text, html);
                } else {
                    sendViaBrevo(recipient, subject, text, html);
                }
                sentTo.add(recipient);
            } catch (Exception e) {
                // Never log addresses, the API key or the email body
                log.error("Failed to send daily report for user {} to recipient {} of {}: {} {}",
                        userId, i + 1, recipients.size(), e.getClass().getSimpleName(), redact(e.getMessage()));
                failedTo.add(recipient);
            }
        }
        return new SendResult(sentTo, failedTo);
    }

    private List<String> getRecipients() {
        return Arrays.stream(reportRecipients.split(","))
                .map(String::trim)
                .filter(address -> !address.isEmpty())
                .toList();
    }

    private void sendViaSmtp(String recipient, String subject, String text, String html) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        if (!fromAddress.isBlank()) helper.setFrom(fromAddress, "SmartPrep");
        helper.setTo(recipient);
        helper.setSubject(subject);
        helper.setText(text, html);
        mailSender.send(message);
    }

    /** POST https://api.brevo.com/v3/smtp/email; any 2xx (201 Created) counts as sent. */
    private void sendViaBrevo(String recipient, String subject, String text, String html) {
        if (brevoFromEmail.isBlank()) {
            throw new IllegalStateException("MAIL_FROM_EMAIL is not set");
        }
        Map<String, Object> payload = Map.of(
                "sender", Map.of("name", brevoFromName, "email", brevoFromEmail),
                "to", List.of(Map.of("email", recipient)),
                "subject", subject,
                "htmlContent", html,
                "textContent", text);

        ResponseEntity<String> response = webClient.post()
                .uri(brevoBaseUrl + "/v3/smtp/email")
                .header("api-key", brevoApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchangeToMono(r -> r.toEntity(String.class))
                .block(BREVO_TIMEOUT);

        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
            int status = response == null ? 0 : response.getStatusCode().value();
            throw new IllegalStateException("Brevo returned HTTP " + status + ": "
                    + brevoErrorMessage(response == null ? null : response.getBody()));
        }
    }

    /** Extracts Brevo's {"code","message"} error text; never the raw body. */
    private static String brevoErrorMessage(String body) {
        try {
            JsonNode node = JSON.readTree(body == null ? "" : body);
            if (node != null && node.hasNonNull("message")) {
                return node.path("code").asText("") + " " + node.get("message").asText();
            }
        } catch (Exception ignored) {
            // not JSON
        }
        return "(no error message)";
    }

    /** Masks anything that looks like an email address. */
    private static String redact(String text) {
        return text == null ? "" : EMAIL_PATTERN.matcher(text).replaceAll("<email>");
    }

    /** Sums hoursStudied over the last 7 days (including today); 0 if anything goes wrong. */
    private double getWeekHours(int userId) {
        try {
            double total = 0;
            for (Map<String, Object> day : studyLogService.getWeeklyHours(userId)) {
                if (day.get("hoursStudied") instanceof Number hours) total += hours.doubleValue();
            }
            return Math.round(total * 10) / 10.0;
        } catch (Exception e) {
            log.warn("Could not load daily study hours for user {}", userId, e);
            return 0;
        }
    }

    /** Plain-text fallback for mail clients that don't render HTML. */
    private String buildPlainText(DashboardDTO d, double weekHours) {
        StringBuilder sb = new StringBuilder()
                .append("SmartPrep Weekly Report for ").append(d.getName()).append("\n\n")
                .append("Current streak: ").append(d.getCurrentStreak()).append(" days\n")
                .append("Average quiz score: ").append(formatPercent(d.getAverageScore())).append("\n")
                .append("Quizzes taken: ").append(d.getTotalQuizzesTaken()).append("\n")
                .append("Study hours this week: ").append(weekHours).append("\n\n")
                .append("Weak topics:\n");
        List<WeakTopicDTO> weak = d.getWeakTopics() == null ? List.of() : d.getWeakTopics();
        if (weak.isEmpty()) sb.append("  None – great work!\n");
        for (WeakTopicDTO t : weak) {
            sb.append("  - ").append(t.getSubject()).append(" – ").append(t.getChapter())
                    .append(": ").append(formatPercent(t.getAverageScore())).append("\n");
        }
        return sb.append("\n").append(d.getMotivationMessage()).toString();
    }

    /** Builds the HTML email body. Uses tables and inline styles so it renders in Gmail and Outlook. */
    private String buildHtml(DashboardDTO d, double weekHours) {
        String name = esc(d.getName());
        String motivation = esc(d.getMotivationMessage());
        String today = LocalDate.now().format(DATE_FORMAT);

        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f3f0fa;font-family:Arial,Helvetica,sans-serif;color:#2d2640;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f3f0fa;padding:24px 12px;">
                    <tr><td align="center">
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:600px;background:#ffffff;border-radius:12px;overflow:hidden;">
                        <tr>
                          <td style="background:#6d28d9;padding:28px 32px;color:#ffffff;">
                            <div style="font-size:24px;font-weight:bold;letter-spacing:0.5px;">SmartPrep</div>
                            <div style="font-size:14px;opacity:0.85;margin-top:4px;">Weekly Progress Report &middot; %s</div>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:28px 32px 8px;">
                            <div style="font-size:20px;font-weight:bold;color:#4c1d95;">%s</div>
                            <div style="font-size:15px;color:#6b6480;margin-top:6px;">&#128293; %d-day study streak</div>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:16px 32px;">
                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;font-size:15px;">
                              %s
                              %s
                              %s
                              %s
                            </table>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:8px 32px 16px;">
                            <div style="font-size:16px;font-weight:bold;color:#4c1d95;margin-bottom:10px;">Topics to focus on</div>
                            %s
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:8px 32px 28px;">
                            <div style="background:#ede9fe;border-left:4px solid #7c3aed;border-radius:8px;padding:16px 18px;font-size:15px;line-height:1.5;color:#3b2a66;">
                              %s
                            </div>
                          </td>
                        </tr>
                        <tr>
                          <td style="background:#faf8ff;padding:16px 32px;font-size:12px;color:#9a93ad;text-align:center;">
                            Sent by SmartPrep &middot; NEET study dashboard
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                today,
                name,
                d.getCurrentStreak(),
                statRow("Current streak", d.getCurrentStreak() + " days"),
                statRow("Average quiz score", formatPercent(d.getAverageScore())),
                statRow("Quizzes taken", String.valueOf(d.getTotalQuizzesTaken())),
                statRow("Study hours this week", weekHours + " h"),
                weakTopicsHtml(d.getWeakTopics()),
                motivation
        );
    }

    private String statRow(String label, String value) {
        return """
                <tr>
                  <td style="padding:12px 14px;border-bottom:1px solid #ede9fe;color:#6b6480;">%s</td>
                  <td style="padding:12px 14px;border-bottom:1px solid #ede9fe;text-align:right;font-weight:bold;color:#4c1d95;">%s</td>
                </tr>
                """.formatted(esc(label), esc(value));
    }

    private String weakTopicsHtml(List<WeakTopicDTO> topics) {
        if (topics == null || topics.isEmpty()) {
            return "<div style=\"font-size:15px;color:#15803d;\">No weak topics right now &ndash; great work! &#127881;</div>";
        }
        StringBuilder sb = new StringBuilder(
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;font-size:14px;\">");
        for (WeakTopicDTO t : topics) {
            boolean weak = t.getAverageScore() < WEAK_THRESHOLD;
            String bg = weak ? "#fef2f2" : "#f0fdf4";
            String color = weak ? "#b91c1c" : "#15803d";
            sb.append("""
                    <tr>
                      <td style="padding:10px 14px;background:%s;border-bottom:4px solid #ffffff;color:#2d2640;">
                        <strong>%s</strong> &ndash; %s
                      </td>
                      <td style="padding:10px 14px;background:%s;border-bottom:4px solid #ffffff;text-align:right;font-weight:bold;color:%s;">%s</td>
                    </tr>
                    """.formatted(bg, esc(t.getSubject()), esc(t.getChapter()), bg, color, formatPercent(t.getAverageScore())));
        }
        return sb.append("</table>").toString();
    }

    private static String formatPercent(double value) {
        return String.format("%.1f%%", value);
    }

    private static String esc(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }
}
