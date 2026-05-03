package com.smartprep.repository;

import com.smartprep.model.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findBySubjectAndChapter(String subject, String chapter);
}