package com.smartprep;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface StudyLogRepository extends JpaRepository<StudyLog, Integer> {
    List<StudyLog> findByUserIdOrderByStudyDateDesc(int userId);
}