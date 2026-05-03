package com.smartprep.controller;

import com.smartprep.model.StudyLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.smartprep.service.StudyLogService;

@RestController
@RequestMapping("/api/study")
public class StudyLogController {

    @Autowired
    private StudyLogService studyLogService;

    @PostMapping("/log")
    public ResponseEntity<String> saveLog(@RequestBody StudyLog log) {
        studyLogService.saveLog(log);
        return ResponseEntity.ok("Study log saved successfully!");
    }

    @GetMapping("/logs/{userId}")
    public ResponseEntity<List<StudyLog>> getLogs(@PathVariable int userId) {
        return ResponseEntity.ok(studyLogService.getLogs(userId));
    }

    @GetMapping("/streak/{userId}")
    public ResponseEntity<String> getStreak(@PathVariable int userId) {
        int streak = studyLogService.getStreak(userId);
        return ResponseEntity.ok("Current streak: " + streak + " days!");
    }
}