package com.smartprep.service;

import com.smartprep.dto.WeakTopicDTO;
import com.smartprep.model.QuizResult;
import com.smartprep.repository.QuizResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class QuizResultService {

    @Autowired
    private QuizResultRepository quizResultRepository;

    public QuizResult saveResult(int userId, String subject, String chapter, int score, int totalQuestions) {
        QuizResult result = new QuizResult();
        result.setUserId(userId);
        result.setSubject(subject);
        result.setChapter(chapter);
        result.setScore(score);
        result.setTotalQuestions(totalQuestions);
        result.setAttemptedAt(LocalDateTime.now());
        return quizResultRepository.save(result);
    }

    // Now returns clean DTOs instead of raw Object[]
    public List<WeakTopicDTO> getWeakTopics(int userId) {
        return quizResultRepository.findWeakTopics(userId);
    }

    public List<QuizResult> getResultsByUser(int userId) {
        return quizResultRepository.findByUserId(userId);
    }
}