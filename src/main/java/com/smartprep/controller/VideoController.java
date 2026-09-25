package com.smartprep.controller;

import com.smartprep.dto.VideoDTO;
import com.smartprep.service.AttentionCheckService;
import com.smartprep.service.VideoRecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * REST endpoints for YouTube video recommendations, search and attention checks.
 */
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/video")
public class VideoController {

    private static final Logger log = LoggerFactory.getLogger(VideoController.class);

    @Autowired
    private VideoRecommendationService videoRecommendationService;

    @Autowired
    private AttentionCheckService attentionCheckService;

    /**
     * Recommends videos for the user's weak topics (based on quiz results).
     *
     * @param userId the user's id
     * @return list of recommended videos; empty list if none or on error
     */
    @GetMapping("/recommend/{userId}")
    public ResponseEntity<List<VideoDTO>> recommendVideos(@PathVariable int userId) {
        try {
            return ResponseEntity.ok(videoRecommendationService.recommendVideos(userId));
        } catch (Exception e) {
            log.error("Failed to recommend videos for user {}", userId, e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Searches YouTube for NEET videos by subject and topic.
     * Example: GET /api/video/search?subject=Physics&chapter=Newton's Laws
     *
     * @param subject optional subject filter (Physics, Chemistry, Biology); empty means all subjects
     * @param chapter the search query / topic; empty returns an empty list
     * @return list of matching videos; empty list if none or on error
     */
    @GetMapping("/search")
    public ResponseEntity<List<VideoDTO>> searchVideos(
            @RequestParam(defaultValue = "") String subject,
            @RequestParam(defaultValue = "") String chapter) {
        try {
            return ResponseEntity.ok(videoRecommendationService.searchVideos(subject, chapter));
        } catch (Exception e) {
            log.error("Video search failed (subject='{}', chapter='{}')", subject, chapter, e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Generates an attention-check question for the given subject and chapter.
     *
     * @param subject the subject being studied
     * @param chapter the chapter being studied
     * @return the generated question text
     */
    @GetMapping("/attention-check")
    public ResponseEntity<String> getAttentionCheck(
            @RequestParam String subject,
            @RequestParam String chapter) {
        String question = attentionCheckService.generateAttentionQuestion(subject, chapter);
        return ResponseEntity.ok(question);
    }
}
