package com.smartprep.controller;

import com.smartprep.dto.VideoDTO;
import com.smartprep.service.AttentionCheckException;
import com.smartprep.service.AttentionCheckService;
import com.smartprep.service.VideoRecommendationService;
import com.smartprep.service.YouTubeUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
     * @return list of recommended videos (empty if the user has no weak topics);
     *         503 with a {@code message} if YouTube is unavailable
     */
    @GetMapping("/recommend/{userId}")
    public ResponseEntity<?> recommendVideos(@PathVariable int userId) {
        try {
            return ResponseEntity.ok(videoRecommendationService.recommendVideos(userId));
        } catch (YouTubeUnavailableException e) {
            return youtubeUnavailable(e);
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
     * @return list of matching videos (empty if none); 503 with a {@code message} if YouTube is unavailable
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchVideos(
            @RequestParam(defaultValue = "") String subject,
            @RequestParam(defaultValue = "") String chapter) {
        try {
            return ResponseEntity.ok(videoRecommendationService.searchVideos(subject, chapter));
        } catch (YouTubeUnavailableException e) {
            return youtubeUnavailable(e);
        } catch (Exception e) {
            log.error("Video search failed (subject='{}', chapter='{}')", subject, chapter, e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Suggests topic names for search-box autocomplete.
     * Example: GET /api/video/suggest?q=cel&subject=Biology
     *
     * @param q       what the user has typed so far
     * @param subject optional subject filter; empty or "All" means all subjects
     * @return up to 8 topic names; empty list if none match or on error
     */
    @GetMapping("/suggest")
    public ResponseEntity<List<String>> suggestTopics(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String subject) {
        try {
            return ResponseEntity.ok(videoRecommendationService.suggestTopics(q, subject));
        } catch (Exception e) {
            log.error("Topic suggestion failed (q='{}', subject='{}')", q, subject, e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Returns a multiple-choice attention-check question for the video being watched,
     * served from cached quiz questions where possible.
     * Example: GET /api/video/attention-check?subject=Biology&chapter=Cell Cycle
     *
     * @param subject the video's subject; "General" or blank for search results
     * @param chapter the video's chapter, or the searched text
     * @return 200 with the question; 503 { message } if the AI daily limit is reached and nothing
     *         is cached; 502 { message } for any other failure
     */
    @GetMapping("/attention-check")
    public ResponseEntity<?> getAttentionCheck(
            @RequestParam(defaultValue = "") String subject,
            @RequestParam(defaultValue = "") String chapter) {
        try {
            return ResponseEntity.ok(attentionCheckService.getAttentionQuestion(subject, chapter));
        } catch (AttentionCheckException e) {
            HttpStatus status = e.isQuotaExhausted() ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY;
            return ResponseEntity.status(status).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Attention check failed (subject='{}', chapter='{}')", subject, chapter, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "Couldn't load an attention question right now."));
        }
    }

    private ResponseEntity<Map<String, String>> youtubeUnavailable(YouTubeUnavailableException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", e.getMessage()));
    }
}
