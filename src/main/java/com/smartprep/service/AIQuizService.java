package com.smartprep.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.smartprep.model.QuizQuestion;
import com.smartprep.repository.QuizQuestionRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;
import com.smartprep.model.QuizQuestion;

@Service
public class AIQuizService {
    
    private final QuizQuestionRepository quizRepo;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    
    public AIQuizService(QuizQuestionRepository quizRepo, 
                         ChatClient.Builder chatClientBuilder) {
        this.quizRepo = quizRepo;
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Transactional
    public String generateQuestions(String subject, String chapter) {
        try {
            // 1. Check cache first
            List<QuizQuestion> cached = quizRepo.findBySubjectAndChapter(subject, chapter);
            
            if (!cached.isEmpty()) {
                System.out.println("✅ Returning " + cached.size() + " cached questions for " + subject + " - " + chapter);
                return objectMapper.writeValueAsString(cached);
            }
            
            // 2. Generate questions using Gemini AI
            System.out.println("🤖 Generating new questions via Gemini AI for " + subject + " - " + chapter);
            String prompt = buildPrompt(subject, chapter);
            
            String aiResponse = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            
            System.out.println("📝 AI Response received");
            
            // 3. Parse and save to cache
            List<QuizQuestion> newQuestions = parseQuestions(aiResponse, subject, chapter);
            
            // 4. Save to database
            List<QuizQuestion> savedQuestions = quizRepo.saveAll(newQuestions);
            System.out.println("💾 Saved " + savedQuestions.size() + " questions to database");
            
            // 5. Return JSON response
            return objectMapper.writeValueAsString(savedQuestions);
            
        } catch (Exception e) {
            System.err.println("❌ Error generating questions: " + e.getMessage());
            e.printStackTrace();
            return "{\"error\": \"Failed to generate questions: " + e.getMessage() + "\"}";
        }
    }
    
   private String buildPrompt(String subject, String chapter) {
    return """
    Generate 5 multiple-choice questions for NEET preparation. Subject: %s, Chapter: %s.
    
    Return ONLY valid JSON array. Each object must have:
    - questionText: string
    - options: a JSON string like "[\\"A\\", \\"B\\", \\"C\\", \\"D\\"]"
    - correctAnswer: string
    - explanation: string
    
    Example:
    {
        "questionText": "What is photosynthesis?",
        "options": "[\\"Process of making food\\", \\"Process of respiration\\", \\"Process of reproduction\\", \\"Process of movement\\"]",
        "correctAnswer": "Process of making food",
        "explanation": "Photosynthesis is how plants make food."
    }
    
    Generate 5 questions now:
    """.formatted(subject, chapter);
}
    
    private List<QuizQuestion> parseQuestions(String aiResponse, String subject, String chapter) {
    try {
        // Clean the response - remove any markdown code blocks
        String cleanResponse = aiResponse;
        if (cleanResponse.contains("```json")) {
            cleanResponse = cleanResponse.substring(cleanResponse.indexOf("```json") + 7);
            cleanResponse = cleanResponse.substring(0, cleanResponse.lastIndexOf("```"));
        } else if (cleanResponse.contains("```")) {
            cleanResponse = cleanResponse.substring(cleanResponse.indexOf("```") + 3);
            cleanResponse = cleanResponse.substring(0, cleanResponse.lastIndexOf("```"));
        }
        cleanResponse = cleanResponse.trim();
        
        // Parse as JsonNode first
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(cleanResponse);
        
        List<QuizQuestion> questions = new ArrayList<>();
        
        for (com.fasterxml.jackson.databind.JsonNode node : root) {
            QuizQuestion q = new QuizQuestion();
            q.setSubject(subject);
            q.setChapter(chapter);
            q.setQuestionText(node.get("questionText").asText());
            q.setCorrectAnswer(node.get("correctAnswer").asText());
            q.setExplanation(node.get("explanation").asText());
            
            // Handle options - whether it's array or string
            com.fasterxml.jackson.databind.JsonNode optionsNode = node.get("options");
            if (optionsNode.isArray()) {
                // Convert array to JSON string
                q.setOptions(objectMapper.writeValueAsString(optionsNode));
            } else {
                // Already a string
                q.setOptions(optionsNode.asText());
            }
            
            questions.add(q);
        }
        
        return questions;
        
    } catch (Exception e) {
        throw new RuntimeException("Failed to parse AI response: " + e.getMessage() + "\nResponse was: " + aiResponse, e);
    }
}
}
