package com.smartprep.repository;

import com.smartprep.model.StudyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface StudyLogRepository extends JpaRepository<StudyLog, Integer> {

    /**
     * All study log rows for a user (legacy logs and Pomodoro sessions).
     *
     * @param userId the user's id
     * @return the user's study logs
     */
    List<StudyLog> findByUserId(int userId);

    /**
     * Distinct days the user studied, newest first. Counts Pomodoro STUDY sessions and legacy
     * daily logs (which have no session type); BREAK sessions are excluded.
     *
     * @param userId the user's id
     * @return distinct study dates, newest first
     */
    @Query("SELECT DISTINCT s.studyDate FROM StudyLog s WHERE s.userId = :userId AND s.studyDate IS NOT NULL "
            + "AND (s.sessionType IS NULL OR s.sessionType = 'STUDY') ORDER BY s.studyDate DESC")
    List<LocalDate> findStudyDatesByUserId(@Param("userId") int userId);

    /**
     * Total STUDY-session minutes completed in [start, end).
     *
     * @param userId the user's id
     * @param start  inclusive start
     * @param end    exclusive end
     * @return total minutes; 0 if there are none
     */
    @Query("SELECT COALESCE(SUM(s.durationMinutes), 0) FROM StudyLog s WHERE s.userId = :userId "
            + "AND s.sessionType = 'STUDY' AND s.completedAt >= :start AND s.completedAt < :end")
    long sumMinutesBetween(@Param("userId") int userId,
                           @Param("start") LocalDateTime start,
                           @Param("end") LocalDateTime end);

    /**
     * Pomodoro sessions (STUDY and BREAK) completed at or after the given time, oldest first.
     *
     * @param userId the user's id
     * @param date   inclusive lower bound
     * @return matching sessions, oldest first
     */
    @Query("SELECT s FROM StudyLog s WHERE s.userId = :userId AND s.completedAt >= :date ORDER BY s.completedAt ASC")
    List<StudyLog> findAllByUserIdAndCompletedAtAfter(@Param("userId") int userId,
                                                      @Param("date") LocalDateTime date);

    /**
     * All of a user's rows, most recently completed session first.
     *
     * @param userId the user's id
     * @return the user's rows, newest session first
     */
    List<StudyLog> findAllByUserIdOrderByCompletedAtDesc(int userId);
}
