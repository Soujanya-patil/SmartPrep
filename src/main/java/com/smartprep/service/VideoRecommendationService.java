package com.smartprep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartprep.dto.VideoDTO;
import com.smartprep.dto.WeakTopicDTO;
import com.smartprep.repository.QuizResultRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.ArrayList;
import java.util.List;

@Service
public class VideoRecommendationService {

    @Autowired
    private QuizResultRepository quizResultRepository;

    @Value("${youtube.api.key}")
    private String youtubeApiKey;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://www.googleapis.com")
            .build();

    public VideoRecommendationService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public List<VideoDTO> recommendVideos(int userId) {
        List<WeakTopicDTO> weakTopics = quizResultRepository.findWeakTopics(userId);
        if (weakTopics.isEmpty()) return new ArrayList<>();

        List<VideoDTO> allVideos = new ArrayList<>();
        for (WeakTopicDTO topic : weakTopics) {
            String query = "NEET " + topic.getSubject() + " " + topic.getChapter();
            List<VideoDTO> videos = searchYouTube(query, topic.getSubject(), topic.getChapter());
            allVideos.addAll(videos);
        }
        return allVideos;
    }

    private List<VideoDTO> searchYouTube(String query, String subject, String chapter) {
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/youtube/v3/search")
                            .queryParam("part", "snippet")
                            .queryParam("q", query)
                            .queryParam("type", "video")
                            .queryParam("maxResults", "2")
                            .queryParam("relevanceLanguage", "en")
                            .queryParam("key", youtubeApiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.path("items");

            List<VideoDTO> videos = new ArrayList<>();
            for (JsonNode item : items) {
                String videoId = item.path("id").path("videoId").asText();
                String title = item.path("snippet").path("title").asText();
                String channel = item.path("snippet").path("channelTitle").asText();
                String youtubeUrl = "https://www.youtube.com/watch?v=" + videoId;
                videos.add(new VideoDTO(subject, chapter, title, channel, youtubeUrl));
            }
            return videos;

        } catch (Exception e) {
            System.err.println("YouTube search error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}