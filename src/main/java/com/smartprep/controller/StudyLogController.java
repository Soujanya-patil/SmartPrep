package com.smartprep.controller;

import com.smartprep.model.StudyLog;
import com.smartprep.service.StudyLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * Study log endpoints: legacy daily logs under /api/study and Pomodoro sessions under /api/studylog.
 */
@RestController
@RequestMapping("/api")
public class StudyLogController {

    @Autowired
    private StudyLogService studyLogService;

    /**
     * Saves a legacy daily study log.
     *
     * @param log the log to save
     * @return confirmation message
     */
    @PostMapping("/study/log")
    public ResponseEntity<String> saveLog(@RequestBody StudyLog log) {
        studyLogService.saveLog(log);
        return ResponseEntity.ok("Study log saved successfully!");
    }

    /**
     * Returns all study log rows for a user.
     *
     * @param userId the user's id
     * @return the user's study logs
     */
    @GetMapping("/study/logs/{userId}")
    public ResponseEntity<List<StudyLog>> getLogs(@PathVariable int userId) {
        return ResponseEntity.ok(studyLogService.getLogs(userId));
    }

    /**
     * Returns the user's current streak as a message.
     *
     * @param userId the user's id
     * @return e.g. "Current streak: 5 days!"
     */
    @GetMapping("/study/streak/{userId}")
    public ResponseEntity<String> getStreak(@PathVariable int userId) {
        int streak = studyLogService.getStreak((long) userId).get("currentStreak");
        return ResponseEntity.ok("Current streak: " + streak + " days!");
    }

    /**
     * Total hours studied today.
     *
     * @param userId the user's id
     * @return { date, totalHours }
     */
    @GetMapping("/study/today/{userId}")
    public ResponseEntity<Map<String, Object>> getTodayHours(@PathVariable int userId) {
        return ResponseEntity.ok(studyLogService.getTodayHours(userId));
    }

    /**
     * Hours studied on each of the last 7 days, oldest first.
     *
     * @param userId the user's id
     * @return [ { date, hoursStudied, goalMet }, ... ]
     */
    @GetMapping("/study/week/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getWeeklyHours(@PathVariable int userId) {
        return ResponseEntity.ok(studyLogService.getWeeklyHours(userId));
    }

    /**
     * Saves a completed Pomodoro session.
     * Body: { userId, subject, chapter, durationMinutes, sessionType }
     *
     * @param session the completed session
     * @return the saved session, or 400 with { "error": ... } if the body is invalid
     */
    @PostMapping("/studylog/save")
    public ResponseEntity<?> saveSession(@RequestBody StudyLog session) {
        try {
            return ResponseEntity.ok(studyLogService.saveSession(session));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Total STUDY minutes and session count for today.
     *
     * @param userId the user's id; a missing or non-numeric id returns zeros
     * @return { totalMinutes, sessionCount }
     */
    @GetMapping("/studylog/today/{userId}")
    public ResponseEntity<Map<String, Long>> getToday(@PathVariable String userId) {
        return ResponseEntity.ok(studyLogService.getTodayMinutes(parseUserId(userId)));
    }

    /**
     * Daily STUDY minutes for the last 7 days, oldest first.
     *
     * @param userId the user's id; a missing or non-numeric id returns zero minutes for each day
     * @return [ { date, minutes }, ... ]
     */
    @GetMapping("/studylog/week/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getWeek(@PathVariable String userId) {
        return ResponseEntity.ok(studyLogService.getWeeklyMinutes(parseUserId(userId)));
    }

    /**
     * Total STUDY minutes and distinct study days this month.
     *
     * @param userId the user's id; a missing or non-numeric id returns zeros
     * @return { totalMinutes, daysStudied }
     */
    @GetMapping("/studylog/month/{userId}")
    public ResponseEntity<Map<String, Long>> getMonth(@PathVariable String userId) {
        return ResponseEntity.ok(studyLogService.getMonthlyMinutes(parseUserId(userId)));
    }

    /**
     * Current and longest study streak.
     *
     * @param userId the user's id; a missing or non-numeric id returns zeros
     * @return { currentStreak, longestStreak }
     */
    @GetMapping("/studylog/streak/{userId}")
    public ResponseEntity<Map<String, Integer>> getStudyStreak(@PathVariable String userId) {
        return ResponseEntity.ok(studyLogService.getStreak(parseUserId(userId)));
    }

    /** Parses the path userId, returning null for "null", "undefined" or other non-numeric values. */
    private static Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
