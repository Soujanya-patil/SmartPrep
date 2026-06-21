package com.smartprep.controller;

import com.smartprep.dto.VideoDTO;
import com.smartprep.service.AttentionCheckService;
import com.smartprep.service.VideoRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    @Autowired
    private VideoRecommendationService videoRecommendationService;

    @Autowired
    private AttentionCheckService attentionCheckService;

    @GetMapping("/recommend/{userId}")
    public ResponseEntity<List<VideoDTO>> recommendVideos(@PathVariable int userId) {
        List<VideoDTO> recommendations = videoRecommendationService.recommendVideos(userId);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/attention-check")
    public ResponseEntity<String> getAttentionCheck(
            @RequestParam String subject,
            @RequestParam String chapter) {
        String question = attentionCheckService.generateAttentionQuestion(subject, chapter);
        return ResponseEntity.ok(question);
    }
}