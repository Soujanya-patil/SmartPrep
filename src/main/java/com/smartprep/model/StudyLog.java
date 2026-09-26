package com.smartprep.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A study log row. Holds either a legacy daily log (studyDate / hoursStudied, saved via
 * POST /api/study/log) or a completed Pomodoro session (saved via POST /api/studylog/save).
 */
@Entity
@Table(name = "study_log")
public class StudyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int userId;

    // Legacy daily-log fields (also filled for Pomodoro sessions so the dashboard streak sees them)
    private LocalDate studyDate;
    private double hoursStudied;
    private String subjectsCovered;
    private boolean goalMet;

    // Pomodoro session fields
    private String subject;
    private String chapter;
    private int durationMinutes;
    private String sessionType;
    private LocalDateTime completedAt;

    @Column(name = "is_completed")
    private boolean completed;

    /** @return the row id */
    public int getId() { return id; }
    /** @param id the row id */
    public void setId(int id) { this.id = id; }

    /** @return the owning user's id */
    public int getUserId() { return userId; }
    /** @param userId the owning user's id */
    public void setUserId(int userId) { this.userId = userId; }

    /** @return the day this log counts towards */
    public LocalDate getStudyDate() { return studyDate; }
    /** @param studyDate the day this log counts towards */
    public void setStudyDate(LocalDate studyDate) { this.studyDate = studyDate; }

    /** @return hours studied (legacy daily log) */
    public double getHoursStudied() { return hoursStudied; }
    /** @param hoursStudied hours studied (legacy daily log) */
    public void setHoursStudied(double hoursStudied) { this.hoursStudied = hoursStudied; }

    /** @return free-text subjects covered (legacy daily log) */
    public String getSubjectsCovered() { return subjectsCovered; }
    /** @param subjectsCovered free-text subjects covered (legacy daily log) */
    public void setSubjectsCovered(String subjectsCovered) { this.subjectsCovered = subjectsCovered; }

    /** @return whether the daily goal was met (legacy daily log) */
    public boolean isGoalMet() { return goalMet; }
    /** @param goalMet whether the daily goal was met (legacy daily log) */
    public void setGoalMet(boolean goalMet) { this.goalMet = goalMet; }

    /** @return optional subject, e.g. "Biology" */
    public String getSubject() { return subject; }
    /** @param subject optional subject, e.g. "Biology" */
    public void setSubject(String subject) { this.subject = subject; }

    /** @return optional chapter, e.g. "Cell Division" */
    public String getChapter() { return chapter; }
    /** @param chapter optional chapter, e.g. "Cell Division" */
    public void setChapter(String chapter) { this.chapter = chapter; }

    /** @return session length in minutes */
    public int getDurationMinutes() { return durationMinutes; }
    /** @param durationMinutes session length in minutes */
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    /** @return "STUDY" or "BREAK"; null for legacy daily logs */
    public String getSessionType() { return sessionType; }
    /** @param sessionType "STUDY" or "BREAK" */
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }

    /** @return when the session finished */
    public LocalDateTime getCompletedAt() { return completedAt; }
    /** @param completedAt when the session finished */
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    /** @return whether the session ran to completion (serialized as "isCompleted") */
    @JsonProperty("isCompleted")
    public boolean isCompleted() { return completed; }
    /** @param completed whether the session ran to completion (read from "isCompleted") */
    @JsonProperty("isCompleted")
    public void setCompleted(boolean completed) { this.completed = completed; }
}
