package com.smartprep.controller;

import com.smartprep.model.QuizResult;
import com.smartprep.service.QuizResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import com.smartprep.dto.WeakTopicDTO;

@RestController
@RequestMapping("/api/results")
public class QuizResultController {

    @Autowired
    private QuizResultService quizResultService;

    // Submit a quiz result
    @PostMapping("/submit")
    public ResponseEntity<QuizResult> submitResult(
            @RequestParam int userId,
            @RequestParam String subject,
            @RequestParam String chapter,
            @RequestParam int score,
            @RequestParam int totalQuestions) {
        QuizResult saved = quizResultService.saveResult(userId, subject, chapter, score, totalQuestions);
        return ResponseEntity.ok(saved);
    }

    // Get weak topics for a student
    @GetMapping("/weak-topics/{userId}")
public ResponseEntity<List<WeakTopicDTO>> getWeakTopics(@PathVariable int userId) {
    List<WeakTopicDTO> weakTopics = quizResultService.getWeakTopics(userId);
    return ResponseEntity.ok(weakTopics);
}
    // Get all results for a student
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<QuizResult>> getHistory(@PathVariable int userId) {
        List<QuizResult> results = quizResultService.getResultsByUser(userId);
        return ResponseEntity.ok(results);
    }
}