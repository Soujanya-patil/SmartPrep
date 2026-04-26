package com.smartprep;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {
    @Autowired
    private AIQuizService aiQuizService;
    @GetMapping("/generate")
    public ResponseEntity<String> generateQuiz(
            @RequestParam String subject,
            @RequestParam String chapter) {
        String questions = aiQuizService.generateQuestions(subject, chapter);
        return ResponseEntity.ok(questions);
    }
}