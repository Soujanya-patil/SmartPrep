package com.smartprep.repository;

import com.smartprep.model.StudyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface StudyLogRepository extends JpaRepository<StudyLog, Integer> {

    List<StudyLog> findByUserId(int userId);

    @Query("SELECT s.studyDate FROM StudyLog s WHERE s.userId = :userId ORDER BY s.studyDate DESC")
    List<LocalDate> findStudyDatesByUserId(@Param("userId") int userId);
}