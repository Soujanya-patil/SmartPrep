package com.smartprep.repository;

import com.smartprep.dto.WeakTopicDTO;
import com.smartprep.model.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface QuizResultRepository extends JpaRepository<QuizResult, Integer> {

    List<QuizResult> findByUserId(int userId);

    @Query("SELECT new com.smartprep.dto.WeakTopicDTO(r.subject, r.chapter, AVG(r.score * 100.0 / r.totalQuestions)) " +
           "FROM QuizResult r WHERE r.userId = :userId " +
           "GROUP BY r.subject, r.chapter " +
           "HAVING AVG(r.score * 100.0 / r.totalQuestions) < 60")
    List<WeakTopicDTO> findWeakTopics(@Param("userId") int userId);
}