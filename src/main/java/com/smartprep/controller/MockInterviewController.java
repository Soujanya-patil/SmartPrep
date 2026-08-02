package com.smartprep.controller;

import com.smartprep.dto.InterviewFeedback;
import com.smartprep.dto.InterviewQuestion;
import com.smartprep.dto.InterviewReport;
import com.smartprep.service.MockInterviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interview")
public class MockInterviewController {

    @Autowired
    private MockInterviewService mockInterviewService;

    @GetMapping("/start")
    public ResponseEntity<List<InterviewQuestion>> startInterview(
            @RequestParam(defaultValue = "Software Engineer") String role,
            @RequestParam(defaultValue = "Java, Spring Boot, MySQL, REST APIs") String skills) {
        List<InterviewQuestion> questions = mockInterviewService.generateQuestions(role, skills);
        return ResponseEntity.ok(questions);
    }

    @PostMapping("/evaluate")
    public ResponseEntity<InterviewFeedback> evaluateAnswer(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String answer = request.get("answer");
        String type = request.get("type");
        InterviewFeedback feedback = mockInterviewService.evaluateAnswer(question, answer, type);
        return ResponseEntity.ok(feedback);
    }

    @PostMapping("/report")
    public ResponseEntity<InterviewReport> generateReport(@RequestBody Map<String, Object> request) {
        int totalScore = (int) request.get("totalScore");
        int maxScore = (int) request.get("maxScore");
        String role = (String) request.getOrDefault("role", "Software Engineer");
        InterviewReport report = mockInterviewService.generateReport(totalScore, maxScore, role);
        return ResponseEntity.ok(report);
    }
}