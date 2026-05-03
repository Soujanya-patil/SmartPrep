package com.smartprep.service;

import com.smartprep.dto.WeakTopicDTO;
import com.smartprep.repository.QuizResultRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ChatbotService {

    @Autowired
    private QuizResultRepository quizResultRepository;

    private final ChatClient chatClient;

    public ChatbotService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String chat(int userId, String userMessage) {

        // 1. Fetch weak topics
        List<WeakTopicDTO> weakTopics = quizResultRepository.findWeakTopics(userId);
        StringBuilder weakTopicsText = new StringBuilder();
        if (weakTopics.isEmpty()) {
            weakTopicsText.append("No weak topics detected yet.");
        } else {
            for (WeakTopicDTO topic : weakTopics) {
                weakTopicsText.append(topic.getSubject())
                        .append(" - ").append(topic.getChapter())
                        .append(" (").append(String.format("%.1f", topic.getAverageScore())).append("%) | ");
            }
        }

        // 2. Build smart prompt with student context
        String fullPrompt = """
                You are SmartPrep AI, a friendly and smart study assistant for NEET exam preparation.
                You are talking to a student. Here is their current data:
                Weak Topics: %s
                
                Your job: Answer their NEET questions clearly, give personalized advice based on their 
                weak topics, motivate them, and keep responses concise and student-friendly.
                
                Student says: %s
                """.formatted(weakTopicsText, userMessage);

        // 3. Call Gemini via Spring AI
        try {
            return chatClient.prompt()
                    .user(fullPrompt)
                    .call()
                    .content();
        } catch (Exception e) {
            return "Sorry, I'm having trouble connecting right now. Please try again!";
        }
    }
}