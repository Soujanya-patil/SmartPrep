package com.smartprep;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AIQuizService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .build();

    public String generateQuestions(String subject, String chapter) {
        String prompt = "Generate 5 NEET exam MCQ questions for subject: " + subject +
                       " and chapter: " + chapter +
                       ". Format as JSON array with fields: question, optionA, optionB, optionC, optionD, correctOption. Return only JSON array, nothing else.";

        String requestBody = "{\"contents\": [{\"parts\": [{\"text\": \""
                + prompt.replace("\"", "\\\"")
                + "\"}]}]}";

        try {
            String response = webClient.post()
                    .uri("/v1beta/models/gemini-2.0-flash-lite:generateContent?key=" + apiKey)
                    .header("content-type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            return root.path("candidates").get(0)
                      .path("content")
                      .path("parts").get(0)
                      .path("text").asText();

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}