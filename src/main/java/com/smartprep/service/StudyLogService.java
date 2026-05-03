package com.smartprep.service;

import com.smartprep.model.StudyLog;
import com.smartprep.repository.StudyLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class StudyLogService {

    @Autowired
    private StudyLogRepository studyLogRepository;

    public StudyLog saveLog(StudyLog log) {
        return studyLogRepository.save(log);
    }

    public List<StudyLog> getLogs(int userId) {
        return studyLogRepository.findByUserId(userId);
    }

    public int getStreak(int userId) {
        List<StudyLog> logs = studyLogRepository.findByUserId(userId);
        int streak = 0;
        LocalDate today = LocalDate.now();
        for (int i = 0; i < logs.size(); i++) {
            if (logs.get(i).getStudyDate().equals(today.minusDays(i))) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }
}