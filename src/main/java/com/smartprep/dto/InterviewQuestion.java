package com.smartprep.dto;

public class InterviewQuestion {
    private int questionNumber;
    private String type; // TECHNICAL, APTITUDE, HR
    private String question;
    private String expectedAnswer;
    private String tips;

    public InterviewQuestion() {}

    public InterviewQuestion(int questionNumber, String type, String question,
                              String expectedAnswer, String tips) {
        this.questionNumber = questionNumber;
        this.type = type;
        this.question = question;
        this.expectedAnswer = expectedAnswer;
        this.tips = tips;
    }

    public int getQuestionNumber() { return questionNumber; }
    public void setQuestionNumber(int questionNumber) { this.questionNumber = questionNumber; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getExpectedAnswer() { return expectedAnswer; }
    public void setExpectedAnswer(String expectedAnswer) { this.expectedAnswer = expectedAnswer; }
    public String getTips() { return tips; }
    public void setTips(String tips) { this.tips = tips; }
}