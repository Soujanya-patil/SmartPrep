package com.smartprep.service;

import com.smartprep.dto.DashboardDTO;
import com.smartprep.dto.WeakTopicDTO;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds the daily SmartPrep progress report and emails it to the parent and the student.
 */
@Service
public class EmailReportService {

    private static final Logger log = LoggerFactory.getLogger(EmailReportService.class);

    /** Every report goes to both addresses, each as its own email. */
    public static final List<String> RECIPIENTS = List.of(
            "dwd.shekhar@gmail.com",
            "soujanya.patil2003@gmail.com"
    );

    private static final double WEAK_THRESHOLD = 60.0;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private StudyLogService studyLogService;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    /**
     * Result of sending one report: which recipients got it and which failed.
     *
     * @param sentTo   addresses the email was delivered to the mail server for
     * @param failedTo addresses that could not be sent to
     */
    public record SendResult(List<String> sentTo, List<String> failedTo) {
        /** @return true if every recipient was sent the report */
        public boolean allSent() { return failedTo.isEmpty(); }
    }

    /**
     * Fetches the student's dashboard and daily study hours, then emails the HTML report to
     * every address in {@link #RECIPIENTS}. A failure for one recipient does not stop the other.
     *
     * @param userId the student's user id
     * @return which recipients were sent the report and which failed
     */
    public SendResult sendWeeklyReport(int userId) {
        DashboardDTO dashboard = dashboardService.getDashboard(userId);
        double weekHours = getWeekHours(userId);

        String subject = "SmartPrep Weekly Report – " + dashboard.getName();
        String html = buildHtml(dashboard, weekHours);

        List<String> sentTo = new ArrayList<>();
        List<String> failedTo = new ArrayList<>();
        for (String recipient : RECIPIENTS) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                if (!fromAddress.isBlank()) helper.setFrom(fromAddress, "SmartPrep");
                helper.setTo(recipient);
                helper.setSubject(subject);
                helper.setText(buildPlainText(dashboard, weekHours), html);
                mailSender.send(message);
                sentTo.add(recipient);
            } catch (Exception e) {
                log.error("Failed to send daily report for user {} to {}", userId, recipient, e);
                failedTo.add(recipient);
            }
        }
        return new SendResult(sentTo, failedTo);
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
