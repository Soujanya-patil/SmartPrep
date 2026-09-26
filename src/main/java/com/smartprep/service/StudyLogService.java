package com.smartprep.service;

import com.smartprep.model.StudyLog;
import com.smartprep.repository.StudyLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StudyLogService {

    public static final String STUDY = "STUDY";
    public static final String BREAK = "BREAK";

    /** Upper bound for a single session, to reject obviously bad client data. */
    private static final int MAX_SESSION_MINUTES = 180;

    @Autowired
    private StudyLogRepository studyLogRepository;

    /**
     * Saves a legacy daily study log as sent by the client.
     *
     * @param log the log to save
     * @return the saved log
     */
    public StudyLog saveLog(StudyLog log) {
        return studyLogRepository.save(log);
    }

    /**
     * Returns all study log rows for a user.
     *
     * @param userId the user's id
     * @return the user's study logs
     */
    public List<StudyLog> getLogs(int userId) {
        return studyLogRepository.findByUserId(userId);
    }

    /**
     * Total hours studied today, from each log's studyDate and hoursStudied.
     *
     * @param userId the user's id
     * @return map with "date" (yyyy-MM-dd) and "totalHours"; 0 hours if nothing was logged today
     */
    public Map<String, Object> getTodayHours(int userId) {
        LocalDate today = LocalDate.now();
        double totalHours = studyLogRepository.findByUserId(userId).stream()
                .filter(l -> today.equals(l.getStudyDate()))
                .mapToDouble(StudyLog::getHoursStudied)
                .sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", today.toString());
        result.put("totalHours", roundHours(totalHours));
        return result;
    }

    /**
     * Hours studied on each of the last 7 days (including today), oldest first.
     * Days with no logs are included with 0 hours.
     *
     * @param userId the user's id
     * @return 7 maps with "date" (yyyy-MM-dd), "hoursStudied" and "goalMet"
     */
    public List<Map<String, Object>> getWeeklyHours(int userId) {
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.minusDays(6);

        Map<LocalDate, Double> hoursByDay = new HashMap<>();
        Set<LocalDate> goalMetDays = new HashSet<>();
        for (StudyLog l : studyLogRepository.findByUserId(userId)) {
            LocalDate date = l.getStudyDate();
            if (date == null || date.isBefore(firstDay) || date.isAfter(today)) continue;
            hoursByDay.merge(date, l.getHoursStudied(), Double::sum);
            if (l.isGoalMet()) goalMetDays.add(date);
        }

        List<Map<String, Object>> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = firstDay.plusDays(i);
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", date.toString());
            day.put("hoursStudied", roundHours(hoursByDay.getOrDefault(date, 0.0)));
            day.put("goalMet", goalMetDays.contains(date));
            days.add(day);
        }
        return days;
    }

    /**
     * Saves a completed Pomodoro session. Always inserts a new row, sets completedAt to now and
     * marks the session completed.
     *
     * @param session the session sent by the client (userId, subject, chapter, durationMinutes, sessionType)
     * @return the saved session
     * @throws IllegalArgumentException if userId, durationMinutes or sessionType is invalid
     */
    public StudyLog saveSession(StudyLog session) {
        if (session == null || session.getUserId() <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
        if (session.getDurationMinutes() <= 0 || session.getDurationMinutes() > MAX_SESSION_MINUTES) {
            throw new IllegalArgumentException("durationMinutes must be between 1 and " + MAX_SESSION_MINUTES);
        }
        String type = session.getSessionType() == null ? "" : session.getSessionType().trim().toUpperCase();
        if (!type.equals(STUDY) && !type.equals(BREAK)) {
            throw new IllegalArgumentException("sessionType must be STUDY or BREAK");
        }

        LocalDateTime now = LocalDateTime.now();
        session.setId(0); // never overwrite an existing row
        session.setSessionType(type);
        session.setSubject(blankToNull(session.getSubject()));
        session.setChapter(blankToNull(session.getChapter()));
        session.setCompletedAt(now);
        session.setCompleted(true);

        // Keep legacy fields in sync so the dashboard streak counts Pomodoro study days
        session.setStudyDate(now.toLocalDate());
        session.setHoursStudied(type.equals(STUDY) ? session.getDurationMinutes() / 60.0 : 0);
        session.setSubjectsCovered(session.getSubject());
        session.setGoalMet(false);

        return studyLogRepository.save(session);
    }

    /**
     * Total STUDY minutes and number of STUDY sessions completed today.
     *
     * @param userId the user's id; null or invalid returns zeros
     * @return map with "totalMinutes" and "sessionCount"
     */
    public Map<String, Long> getTodayMinutes(Long userId) {
        long totalMinutes = 0;
        long sessionCount = 0;
        if (isValidUserId(userId)) {
            for (StudyLog s : studySessionsSince(userId.intValue(), LocalDate.now().atStartOfDay())) {
                totalMinutes += s.getDurationMinutes();
                sessionCount++;
            }
        }
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("totalMinutes", totalMinutes);
        result.put("sessionCount", sessionCount);
        return result;
    }

    /**
     * Daily STUDY minutes for the last 7 days (including today), oldest first.
     * Days with no study are included with 0 minutes.
     *
     * @param userId the user's id; null or invalid returns 7 zero entries
     * @return list of maps with "date" (yyyy-MM-dd) and "minutes"
     */
    public List<Map<String, Object>> getWeeklyMinutes(Long userId) {
        LocalDate firstDay = LocalDate.now().minusDays(6);

        Map<LocalDate, Long> minutesByDay = new HashMap<>();
        if (isValidUserId(userId)) {
            for (StudyLog s : studySessionsSince(userId.intValue(), firstDay.atStartOfDay())) {
                minutesByDay.merge(s.getCompletedAt().toLocalDate(), (long) s.getDurationMinutes(), Long::sum);
            }
        }

        List<Map<String, Object>> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = firstDay.plusDays(i);
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", date.toString());
            day.put("minutes", minutesByDay.getOrDefault(date, 0L));
            days.add(day);
        }
        return days;
    }

    /**
     * Total STUDY minutes and number of distinct study days in the current calendar month.
     *
     * @param userId the user's id; null or invalid returns zeros
     * @return map with "totalMinutes" and "daysStudied"
     */
    public Map<String, Long> getMonthlyMinutes(Long userId) {
        long totalMinutes = 0;
        long daysStudied = 0;
        if (isValidUserId(userId)) {
            LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
            LocalDateTime start = firstOfMonth.atStartOfDay();
            LocalDateTime end = firstOfMonth.plusMonths(1).atStartOfDay();

            totalMinutes = studyLogRepository.sumMinutesBetween(userId.intValue(), start, end);
            daysStudied = studySessionsSince(userId.intValue(), start).stream()
                    .map(s -> s.getCompletedAt().toLocalDate())
                    .distinct()
                    .count();
        }
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("totalMinutes", totalMinutes);
        result.put("daysStudied", daysStudied);
        return result;
    }

    /**
     * Current and longest streak of consecutive days with at least one STUDY session
     * (legacy daily logs also count). The current streak stays alive until the end of today,
     * so a user who studied yesterday but not yet today keeps their streak.
     *
     * @param userId the user's id; null or invalid returns zeros
     * @return map with "currentStreak" and "longestStreak"
     */
    public Map<String, Integer> getStreak(Long userId) {
        int current = 0;
        int longest = 0;
        if (isValidUserId(userId)) {
            List<LocalDate> dates = studyLogRepository.findStudyDatesByUserId(userId.intValue()); // distinct, newest first
            Set<LocalDate> studyDays = new HashSet<>(dates);

            LocalDate today = LocalDate.now();
            LocalDate cursor = studyDays.contains(today) ? today : today.minusDays(1);
            while (studyDays.contains(cursor)) {
                current++;
                cursor = cursor.minusDays(1);
            }

            int run = 0;
            LocalDate previous = null;
            for (int i = dates.size() - 1; i >= 0; i--) { // oldest to newest
                LocalDate date = dates.get(i);
                run = (previous != null && date.equals(previous.plusDays(1))) ? run + 1 : 1;
                longest = Math.max(longest, run);
                previous = date;
            }
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("currentStreak", current);
        result.put("longestStreak", longest);
        return result;
    }

    private List<StudyLog> studySessionsSince(int userId, LocalDateTime start) {
        return studyLogRepository.findAllByUserIdAndCompletedAtAfter(userId, start).stream()
                .filter(s -> STUDY.equals(s.getSessionType()))
                .toList();
    }

    private static double roundHours(double hours) {
        return Math.round(hours * 100) / 100.0;
    }

    private static boolean isValidUserId(Long userId) {
        return userId != null && userId > 0 && userId <= Integer.MAX_VALUE;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
