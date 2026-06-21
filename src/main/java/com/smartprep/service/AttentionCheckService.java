package com.smartprep.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AttentionCheckService {

    private final ChatClient chatClient;

    public AttentionCheckService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String generateAttentionQuestion(String subject, String chapter) {
        String prompt = """
                Generate exactly 1 NEET MCQ question for subject: %s, chapter: %s.
                This is an attention check question shown mid-video to test focus.
                
                Return ONLY valid JSON object with these fields:
                - question: string
                - optionA: string
                - optionB: string
                - optionC: string
                - optionD: string
                - correctOption: string (must be exactly "A", "B", "C", or "D")
                - encouragement: string (fun message if correct, like "Flawless! You're on fire! 🔥")
                - hint: string (gentle hint if wrong, like "Almost! Review the mitosis phases 💪")
                
                Return only the JSON object, nothing else.
                """.formatted(subject, chapter);

        try {
            return chatClient.prompt().user(prompt).call().content();
        } catch (Exception e) {
            return "{\"error\": \"Could not generate question right now.\"}";
        }
    }
}