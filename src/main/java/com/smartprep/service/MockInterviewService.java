package com.smartprep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartprep.dto.InterviewFeedback;
import com.smartprep.dto.InterviewQuestion;
import com.smartprep.dto.InterviewReport;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class MockInterviewService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MockInterviewService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public List<InterviewQuestion> generateQuestions(String role, String skills) {
    String prompt = """
        IMPORTANT: You must write questions EXACTLY in this casual style. No formal language allowed.
        
        GOOD examples (copy this exact tone):
        ✅ "Okay so you've used Spring Boot — nice! Walk me through how YOU personally built a REST API. Like step by step, what did you actually do?"
        ✅ "Quick math one 😄 — 8 balls, one is heavier, you have a balance scale. What's the minimum weighings to find it? Think out loud!"
        ✅ "Alright real talk — what's one thing on your resume that you're NOT fully confident about yet? Be honest! 😊"
        ✅ "HashMap vs Hashtable — go! And don't just say 'one is synchronized' — give me a real scenario where you'd choose one over the other."
        
        BAD examples (NEVER write like this):
        ❌ "Explain the concept of Object-Oriented Programming"
        ❌ "What are the four pillars of OOP?"
        ❌ "Describe a time when you faced a challenge"
        
        You are interviewing a fresher for %s role. Their skills: %s.
        Generate 10 questions in the GOOD style above.
        Mix: 4 TECHNICAL (based on their skills: %s), 3 APTITUDE, 3 HR.
        
        Return ONLY a valid JSON array. Each object:
        - questionNumber: number (1-10)
        - type: string (TECHNICAL, APTITUDE, or HR)
        - question: string (MUST be casual like the GOOD examples!)
        - expectedAnswer: string
        - tips: string (casual Alex tip)
        
        JSON array only, no markdown, no backticks.
        """.formatted(role, skills, skills);

    try {
        String response = chatClient.prompt().user(prompt).call().content();
        String cleaned = response.replace("```json", "").replace("```", "").trim();
        JsonNode array = objectMapper.readTree(cleaned);

        List<InterviewQuestion> questions = new ArrayList<>();
        for (JsonNode node : array) {
            InterviewQuestion q = new InterviewQuestion(
                node.path("questionNumber").asInt(),
                node.path("type").asText(),
                node.path("question").asText(),
                node.path("expectedAnswer").asText(),
                node.path("tips").asText()
            );
            questions.add(q);
        }
        return questions;

    } catch (Exception e) {
        System.err.println("Interview generation error: " + e.getMessage());
        return new ArrayList<>();
    }
}

    // Evaluate a single answer
    public InterviewFeedback evaluateAnswer(String question, String userAnswer, String type) {
    String prompt = """
        You are Alex, a witty Senior Software Engineer interviewer.
        You just heard this answer and you're reacting naturally like a real human.
        
        Question Type: %s
        Question: %s
        Candidate's Answer: %s
        
        React like Alex would in real life! Examples:
        - If great: "Okay okay I see you! That was actually really solid 🔥 You clearly know your stuff."
        - If okay: "Not bad! You got the main idea but you missed something important here..."
        - If poor: "Hmm... I can see you tried but let me be honest with you — this needs work."
        
        Return ONLY a valid JSON object with:
        - score: number (0-10)
        - feedback: string (Alex's natural reaction, 2-3 sentences, use emojis!)
        - improvement: string (Alex's specific tip, casual and actionable)
        - passed: boolean (true if score >= 6)
        
        Return only the JSON object, no markdown, no backticks.
        """.formatted(type, question, userAnswer);
    try {
        String response = chatClient.prompt().user(prompt).call().content();
        String cleaned = response.replace("```json", "").replace("```", "").trim();
        JsonNode node = objectMapper.readTree(cleaned);

        return new InterviewFeedback(
            node.path("score").asInt(),
            node.path("feedback").asText(),
            node.path("improvement").asText(),
            node.path("passed").asBoolean()
        );

    } catch (Exception e) {
        return new InterviewFeedback(0, "Could not evaluate answer", "Please try again", false);
    }
}

    // Generate final report
    public InterviewReport generateReport(int totalScore, int maxScore, String role) {
    double percentage = (totalScore * 100.0) / maxScore;
    boolean hired = percentage >= 60;

    String prompt = """
        You are Alex, wrapping up a mock interview session.
        Be real, be human, be encouraging but honest.
        
        Role: %s
        Score: %d/%d (%.1f%%)
        Verdict: %s
        
        Write the final report like Alex would say it in person:
        - If hired: Be genuinely excited! "Okay I'll be real — you impressed me today! 🎉"
        - If not hired: Be warm and encouraging! "Hey, you're not there yet — but I've seen worse and they made it. Here's what to fix..."
        
        Return ONLY a valid JSON object with:
        - overallFeedback: string (Alex's closing statement, personal and real, 2-3 sentences)
        - strengths: array of 3 strings (specific praise, Alex's style)
        - improvements: array of 3 strings (honest actionable advice, Alex's style)
        
        Return only the JSON object, no markdown, no backticks.
        """.formatted(role, totalScore, maxScore, percentage, hired ? "HIRED! 🎉" : "NOT YET — keep grinding!");
    try {
        String response = chatClient.prompt().user(prompt).call().content();
        String cleaned = response.replace("```json", "").replace("```", "").trim();
        JsonNode node = objectMapper.readTree(cleaned);

        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();

        for (JsonNode s : node.path("strengths")) strengths.add(s.asText());
        for (JsonNode i : node.path("improvements")) improvements.add(i.asText());

        return new InterviewReport(
            totalScore, maxScore, percentage,
            node.path("overallFeedback").asText(),
            strengths, improvements, hired
        );

    } catch (Exception e) {
        return new InterviewReport(totalScore, maxScore, percentage,
            "Interview completed!", new ArrayList<>(), new ArrayList<>(), hired);
    }
}
}