package com.smartprep;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class StudyLogService {

    @Autowired
    private StudyLogRepository studyLogRepository;

    public StudyLog saveLog(StudyLog log) {
        log.setStudyDate(LocalDate.now());
        return studyLogRepository.save(log);
    }

    public List<StudyLog> getLogs(int userId) {
        return studyLogRepository.findByUserIdOrderByStudyDateDesc(userId);
    }

    public int getStreak(int userId) {
        List<StudyLog> logs = studyLogRepository.findByUserIdOrderByStudyDateDesc(userId);
        int streak = 0;
        LocalDate expected = LocalDate.now();
        for (StudyLog log : logs) {
            if (log.getStudyDate().equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }
}