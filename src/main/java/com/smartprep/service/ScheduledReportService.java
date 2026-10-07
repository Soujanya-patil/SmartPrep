package com.smartprep.service;

import com.smartprep.service.EmailReportService.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Sends the student's progress report email automatically every night.
 * Disabled with report.scheduler.enabled=false (on Render an external cron calls
 * POST /api/report/cron/send instead, because the free instance sleeps).
 */
@Service
@ConditionalOnProperty(name = "report.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledReportService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledReportService.class);

    /** The student whose report is emailed each night (also used by the cron endpoint). */
    public static final int STUDENT_USER_ID = 1;

    @Autowired
    private EmailReportService emailReportService;

    /**
     * Every day at 10:00 PM IST (Asia/Kolkata), regardless of the server's own time zone.
     * Errors are logged and never propagate, so a failed send can't stop future runs.
     */
    @Scheduled(cron = "0 0 22 * * *", zone = "Asia/Kolkata")
    public void sendDailyReport() {
        log.info("Sending daily report for user {} (10 PM IST)...", STUDENT_USER_ID);
        try {
            SendResult result = emailReportService.sendWeeklyReport(STUDENT_USER_ID);
            if (result.allSent()) {
                log.info("Daily report sent to {} recipient(s)", result.sentTo().size());
            } else {
                log.error("Daily report failed for {} recipient(s) (sent to {})",
                        result.failedTo().size(), result.sentTo().size());
            }
        } catch (Exception e) {
            log.error("Failed to send daily report", e);
        }
    }
}
