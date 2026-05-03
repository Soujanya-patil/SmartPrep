package com.smartprep.repository;

import com.smartprep.model.StudyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudyLogRepository extends JpaRepository<StudyLog, Integer> {
    List<StudyLog> findByUserId(int userId);
}