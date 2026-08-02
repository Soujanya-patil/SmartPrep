package com.smartprep.dto;

import java.util.List;

public class InterviewReport {
    private int totalScore;
    private int maxScore;
    private double percentage;
    private String overallFeedback;
    private List<String> strengths;
    private List<String> improvements;
    private boolean hired;

    public InterviewReport() {}

    public InterviewReport(int totalScore, int maxScore, double percentage,
                           String overallFeedback, List<String> strengths,
                           List<String> improvements, boolean hired) {
        this.totalScore = totalScore;
        this.maxScore = maxScore;
        this.percentage = percentage;
        this.overallFeedback = overallFeedback;
        this.strengths = strengths;
        this.improvements = improvements;
        this.hired = hired;
    }

    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
    public int getMaxScore() { return maxScore; }
    public void setMaxScore(int maxScore) { this.maxScore = maxScore; }
    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }
    public String getOverallFeedback() { return overallFeedback; }
    public void setOverallFeedback(String overallFeedback) { this.overallFeedback = overallFeedback; }
    public List<String> getStrengths() { return strengths; }
    public void setStrengths(List<String> strengths) { this.strengths = strengths; }
    public List<String> getImprovements() { return improvements; }
    public void setImprovements(List<String> improvements) { this.improvements = improvements; }
    public boolean isHired() { return hired; }
    public void setHired(boolean hired) { this.hired = hired; }
}