package com.smartprep;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "study_log")
public class StudyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int userId;
    private LocalDate studyDate;
    private double hoursStudied;
    private String subjectsCovered;
    private boolean goalMet;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public LocalDate getStudyDate() { return studyDate; }
    public void setStudyDate(LocalDate studyDate) { this.studyDate = studyDate; }

    public double getHoursStudied() { return hoursStudied; }
    public void setHoursStudied(double hoursStudied) { this.hoursStudied = hoursStudied; }

    public String getSubjectsCovered() { return subjectsCovered; }
    public void setSubjectsCovered(String subjectsCovered) { this.subjectsCovered = subjectsCovered; }

    public boolean isGoalMet() { return goalMet; }
    public void setGoalMet(boolean goalMet) { this.goalMet = goalMet; }
}