package com.smartprep.dto;

public class WeakTopicDTO {
    private String subject;
    private String chapter;
    private double averageScore;

    // Constructor
    public WeakTopicDTO(String subject, String chapter, double averageScore) {
        this.subject = subject;
        this.chapter = chapter;
        this.averageScore = averageScore;
    }

    // Getters
    public String getSubject() { return subject; }
    public String getChapter() { return chapter; }
    public double getAverageScore() { return averageScore; }

    // toString for debugging
    @Override
    public String toString() {
        return "WeakTopicDTO{" +
                "subject='" + subject + '\'' +
                ", chapter='" + chapter + '\'' +
                ", averageScore=" + String.format("%.1f%%", averageScore) +
                '}';
    }
}