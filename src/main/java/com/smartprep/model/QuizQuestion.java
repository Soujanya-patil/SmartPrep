package com.smartprep.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String subject;
    private String chapter;
    
    @JsonProperty("questionText")
    @Column(length = 1000)
    private String questionText;
    
    @Column(length = 2000)
    private String options;
    
    private String correctAnswer;
    
    @Column(length = 1000)
    private String explanation;
    
    public QuizQuestion() {}
    
    // Special setter for options that handles both String and Array
    public void setOptions(Object options) {
        try {
            if (options instanceof String) {
                this.options = (String) options;
            } else {
                // Convert to JSON string
                this.options = objectMapper.writeValueAsString(options);
            }
        } catch (Exception e) {
            this.options = options.toString();
        }
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getChapter() { return chapter; }
    public void setChapter(String chapter) { this.chapter = chapter; }
    
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    
    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }
    
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}