package com.smartprep.dto;

public class InterviewFeedback {
    private int score; // out of 10
    private String feedback;
    private String improvement;
    private boolean passed;

    public InterviewFeedback() {}

    public InterviewFeedback(int score, String feedback, String improvement, boolean passed) {
        this.score = score;
        this.feedback = feedback;
        this.improvement = improvement;
        this.passed = passed;
    }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public String getImprovement() { return improvement; }
    public void setImprovement(String improvement) { this.improvement = improvement; }
    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }
}