package com.smartprep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartprep.dto.VideoDTO;
import com.smartprep.dto.WeakTopicDTO;
import com.smartprep.repository.QuizResultRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class VideoRecommendationService {

    @Autowired
    private QuizResultRepository quizResultRepository;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VideoRecommendationService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public List<VideoDTO> recommendVideos(int userId) {
        // 1. Get weak topics
        List<WeakTopicDTO> weakTopics = quizResultRepository.findWeakTopics(userId);

        if (weakTopics.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. Build topic list
        StringBuilder topicList = new StringBuilder();
        for (WeakTopicDTO topic : weakTopics) {
            topicList.append(topic.getSubject())
                    .append(" - ").append(topic.getChapter()).append(", ");
        }

        // 3. Ask AI for YouTube recommendations
        String prompt = """
                A NEET student has the following weak topics: %s
                
                For each topic, recommend 2 high quality YouTube videos.
                Return ONLY a valid JSON array with no markdown. Each object must have exactly these fields:
                - subject: string
                - chapter: string
                - videoTitle: string
                - channel: string
                - youtubeUrl: string (real YouTube URL like https://www.youtube.com/watch?v=XXXXXXXXXXX)
                
                Only return the JSON array, nothing else, no markdown, no backticks.
                """.formatted(topicList);

        try {
            String response = chatClient.prompt().user(prompt).call().content();
            String cleaned = response.replace("```json", "").replace("```", "").trim();
            JsonNode array = objectMapper.readTree(cleaned);

            List<VideoDTO> videos = new ArrayList<>();
            for (JsonNode node : array) {
                VideoDTO dto = new VideoDTO(
                    node.path("subject").asText(),
                    node.path("chapter").asText(),
                    node.path("videoTitle").asText(),
                    node.path("channel").asText(),
                    node.path("youtubeUrl").asText()
                );
                videos.add(dto);
            }
            return videos;

        } catch (Exception e) {
            System.err.println("Video recommendation error: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}