package com.smartprep.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.smartprep.model.QuizQuestion;
import com.smartprep.repository.QuizQuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;
import com.smartprep.model.QuizQuestion;

@Service
public class AIQuizService {

    private static final Logger log = LoggerFactory.getLogger(AIQuizService.class);

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
                log.info("Returning {} cached questions for {} - {}", cached.size(), subject, chapter);
                return objectMapper.writeValueAsString(cached);
            }

            // 2. Generate with Gemini AI and save to the cache
            List<QuizQuestion> savedQuestions = generateAndSaveQuestions(subject, chapter);

            // 3. Return JSON response
            return objectMapper.writeValueAsString(savedQuestions);

        } catch (Exception e) {
            log.error("Error generating questions for {} - {}", subject, chapter, e);
            return "{\"error\": \"Failed to generate questions: " + e.getMessage() + "\"}";
        }
    }

    /**
     * Asks Gemini for 10 questions on a chapter and saves them to quiz_questions, so later
     * requests for the same chapter are served from the database. Makes exactly one AI call.
     *
     * @param subject the subject the questions are saved under
     * @param chapter the chapter the questions are saved under
     * @return the saved questions
     * @throws RuntimeException if the AI call fails (e.g. daily quota reached) or its reply can't be parsed
     */
    public List<QuizQuestion> generateAndSaveQuestions(String subject, String chapter) {
        log.info("Generating new questions via Gemini AI for {} - {}", subject, chapter);
        String aiResponse = chatClient.prompt()
                .user(buildPrompt(subject, chapter))
                .call()
                .content();
        log.info("AI response received for {} - {}", subject, chapter);

        List<QuizQuestion> savedQuestions = quizRepo.saveAll(parseQuestions(aiResponse, subject, chapter));
        log.info("Saved {} questions to database for {} - {}", savedQuestions.size(), subject, chapter);
        return savedQuestions;
    }
    
   private String buildPrompt(String subject, String chapter) {
    return """
    Generate 10 multiple-choice questions for NEET preparation. Subject: %s, Chapter: %s.
    
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
    
    Generate 10 questions now:
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
