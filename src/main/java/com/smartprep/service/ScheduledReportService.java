package com.smartprep.service;

import com.smartprep.service.EmailReportService.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Sends the student's progress report email automatically every night.
 */
@Service
public class ScheduledReportService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledReportService.class);

    /** The student whose report is emailed each night. */
    private static final int STUDENT_USER_ID = 1;

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
                log.info("Daily report sent to {}", result.sentTo());
            } else {
                log.error("Daily report failed for {} (sent to {})", result.failedTo(), result.sentTo());
            }
        } catch (Exception e) {
            log.error("Failed to send daily report", e);
        }
    }
}
