package com.smartprep.dto;

import java.util.List;

public class DashboardDTO {
    private String name;
    private String email;
    private int currentStreak;
    private int totalQuizzesTaken;
    private double averageScore;
    private List<WeakTopicDTO> weakTopics;
    private String motivationMessage;

    public DashboardDTO(String name, String email, int currentStreak,
                        int totalQuizzesTaken, double averageScore,
                        List<WeakTopicDTO> weakTopics, String motivationMessage) {
        this.name = name;
        this.email = email;
        this.currentStreak = currentStreak;
        this.totalQuizzesTaken = totalQuizzesTaken;
        this.averageScore = averageScore;
        this.weakTopics = weakTopics;
        this.motivationMessage = motivationMessage;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public int getCurrentStreak() { return currentStreak; }
    public int getTotalQuizzesTaken() { return totalQuizzesTaken; }
    public double getAverageScore() { return averageScore; }
    public List<WeakTopicDTO> getWeakTopics() { return weakTopics; }
    public String getMotivationMessage() { return motivationMessage; }
}