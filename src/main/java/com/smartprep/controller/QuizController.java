package com.smartprep.controller;

import com.smartprep.service.AIQuizService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {
    
    private final AIQuizService quizService;
    
    public QuizController(AIQuizService quizService) {
        this.quizService = quizService;
    }
    
    @GetMapping("/generate")
    public String generateQuestions(@RequestParam String subject, 
                                     @RequestParam String chapter) {
        return quizService.generateQuestions(subject, chapter);
    }
}