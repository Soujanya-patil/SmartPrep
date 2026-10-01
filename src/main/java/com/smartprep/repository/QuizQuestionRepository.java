package com.smartprep.repository;

import com.smartprep.model.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;


public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findBySubjectAndChapter(String subject, String chapter);

    /** Cached questions for a subject + chapter, ignoring case and surrounding spaces on both sides. */
    @Query("SELECT q FROM QuizQuestion q WHERE LOWER(TRIM(q.subject)) = LOWER(TRIM(:subject)) "
            + "AND LOWER(TRIM(q.chapter)) = LOWER(TRIM(:chapter))")
    List<QuizQuestion> findBySubjectIgnoreCaseAndChapterIgnoreCase(@Param("subject") String subject,
                                                                   @Param("chapter") String chapter);

    /** Cached questions for a subject (any chapter), ignoring case and surrounding spaces. */
    @Query("SELECT q FROM QuizQuestion q WHERE LOWER(TRIM(q.subject)) = LOWER(TRIM(:subject))")
    List<QuizQuestion> findBySubjectIgnoreCase(@Param("subject") String subject);

    /**
     * Cached questions in any subject whose chapter contains the given text, ignoring case.
     * The text must already have LIKE wildcards escaped with '!' (see AttentionCheckService).
     */
    @Query("SELECT q FROM QuizQuestion q WHERE LOWER(q.chapter) LIKE LOWER(CONCAT('%', :text, '%')) ESCAPE '!'")
    List<QuizQuestion> findByChapterContainingIgnoreCase(@Param("text") String text);
}
