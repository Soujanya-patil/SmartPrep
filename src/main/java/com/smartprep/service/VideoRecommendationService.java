package com.smartprep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartprep.dto.VideoDTO;
import com.smartprep.dto.WeakTopicDTO;
import com.smartprep.repository.QuizResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Finds NEET study videos on YouTube, either from a user's weak topics or from a free-text search.
 */
@Service
public class VideoRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(VideoRecommendationService.class);

    /** Videos fetched per weak topic on the recommendations list. */
    private static final int RECOMMEND_MAX_RESULTS = 2;

    /** Videos fetched for a user-initiated search. */
    private static final int SEARCH_MAX_RESULTS = 10;

    /** Topic suggestions returned for one autocomplete query. */
    private static final int SUGGEST_MAX_RESULTS = 8;

    @Autowired
    private QuizResultRepository quizResultRepository;

    @Value("${youtube.api.key}")
    private String youtubeApiKey;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://www.googleapis.com")
            .build();

    /**
     * Creates the service with a Spring AI chat client.
     *
     * @param chatClientBuilder builder supplied by Spring AI auto-configuration
     */
    public VideoRecommendationService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Recommends videos for each of the user's weak topics.
     *
     * @param userId the user's id
     * @return videos for all weak topics; empty list if the user has none
     */
    public List<VideoDTO> recommendVideos(int userId) {
        List<WeakTopicDTO> weakTopics = quizResultRepository.findWeakTopics(userId);
        if (weakTopics.isEmpty()) return new ArrayList<>();

        List<VideoDTO> allVideos = new ArrayList<>();
        for (WeakTopicDTO topic : weakTopics) {
            String query = "NEET " + topic.getSubject() + " " + topic.getChapter();
            allVideos.addAll(searchYouTube(query, topic.getSubject(), topic.getChapter(), RECOMMEND_MAX_RESULTS));
        }
        return allVideos;
    }

    /**
     * Searches YouTube for NEET videos on a topic, optionally narrowed by subject.
     *
     * @param subject optional subject; null or blank means all subjects
     * @param chapter the search query / topic; null or blank returns an empty list
     * @return matching videos; empty list if nothing is found or the search fails
     */
    public List<VideoDTO> searchVideos(String subject, String chapter) {
        String cleanSubject = subject == null ? "" : subject.trim();
        String cleanChapter = chapter == null ? "" : chapter.trim();
        if (cleanChapter.isEmpty()) return new ArrayList<>();

        String query = cleanSubject.isEmpty()
                ? "NEET " + cleanChapter
                : "NEET " + cleanSubject + " " + cleanChapter;
        return searchYouTube(query, cleanSubject, cleanChapter, SEARCH_MAX_RESULTS);
    }

    /**
     * Suggests topic names for search-box autocomplete from the local NEET/JEE topic catalog.
     * No YouTube call is made, so this is safe to hit on every keystroke.
     * Ranking: topics that start with the query, then topics with a word starting with it,
     * then any other topic containing it.
     *
     * @param query   what the user has typed so far; blank returns an empty list
     * @param subject optional subject filter (Physics, Chemistry, Maths, Biology); blank or "All" means all
     * @return up to {@value #SUGGEST_MAX_RESULTS} distinct topic names, best matches first
     */
    public List<String> suggestTopics(String query, String subject) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return new ArrayList<>();

        String cleanSubject = subject == null ? "" : subject.trim();
        boolean allSubjects = cleanSubject.isEmpty() || cleanSubject.equalsIgnoreCase("All");

        List<String> startsWith = new ArrayList<>();
        List<String> wordStartsWith = new ArrayList<>();
        List<String> contains = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : NeetTopicCatalog.TOPICS_BY_SUBJECT.entrySet()) {
            if (!allSubjects && !entry.getKey().equalsIgnoreCase(cleanSubject)) continue;

            for (String topic : entry.getValue()) {
                String lower = topic.toLowerCase(Locale.ROOT);
                if (lower.startsWith(q)) startsWith.add(topic);
                else if (lower.contains(" " + q) || lower.contains("-" + q)) wordStartsWith.add(topic);
                else if (lower.contains(q)) contains.add(topic);
            }
        }

        // Shorter names first within each tier so "Cell Cycle" beats "Cell: The Unit of Life"
        startsWith.sort((a, b) -> Integer.compare(a.length(), b.length()));
        wordStartsWith.sort((a, b) -> Integer.compare(a.length(), b.length()));
        contains.sort((a, b) -> Integer.compare(a.length(), b.length()));

        // LinkedHashSet drops topics listed under two subjects (e.g. Biomolecules) while keeping order
        Set<String> ranked = new LinkedHashSet<>();
        ranked.addAll(startsWith);
        ranked.addAll(wordStartsWith);
        ranked.addAll(contains);
        return ranked.stream().limit(SUGGEST_MAX_RESULTS).toList();
    }

    /**
     * Calls the YouTube Data API search endpoint and maps results to VideoDTOs with embed URLs.
     *
     * @param query      the full search query sent to YouTube
     * @param subject    subject to tag each result with
     * @param chapter    chapter to tag each result with
     * @param maxResults maximum number of videos to return
     * @return list of videos; empty list on any error
     */
    private List<VideoDTO> searchYouTube(String query, String subject, String chapter, int maxResults) {
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/youtube/v3/search")
                            .queryParam("part", "snippet")
                            .queryParam("q", query)
                            .queryParam("type", "video")
                            .queryParam("maxResults", maxResults)
                            .queryParam("videoEmbeddable", "true")
                            .queryParam("relevanceLanguage", "en")
                            .queryParam("key", youtubeApiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null) return new ArrayList<>();

            JsonNode items = objectMapper.readTree(response).path("items");

            List<VideoDTO> videos = new ArrayList<>();
            for (JsonNode item : items) {
                String videoId = item.path("id").path("videoId").asText();
                if (videoId.isEmpty()) continue;

                String title = item.path("snippet").path("title").asText();
                String channel = item.path("snippet").path("channelTitle").asText();
                String youtubeUrl = "https://www.youtube.com/embed/" + videoId;
                String thumbnail = "https://img.youtube.com/vi/" + videoId + "/0.jpg";
                videos.add(new VideoDTO(subject, chapter, title, channel, youtubeUrl, thumbnail));
            }
            return videos;

        } catch (Exception e) {
            log.error("YouTube search failed for query '{}'", query, e);
            return new ArrayList<>();
        }
    }
}
