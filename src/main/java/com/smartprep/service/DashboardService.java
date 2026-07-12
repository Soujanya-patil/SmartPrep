package com.smartprep.service;

import com.smartprep.dto.DashboardDTO;
import com.smartprep.dto.WeakTopicDTO;
import com.smartprep.model.User;
import com.smartprep.repository.QuizResultRepository;
import com.smartprep.repository.StudyLogRepository;
import com.smartprep.repository.UserRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class DashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuizResultRepository quizResultRepository;

    @Autowired
    private StudyLogRepository studyLogRepository;

    private final ChatClient chatClient;

    public DashboardService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public DashboardDTO getDashboard(int userId) {

        // 1. Get user details
        Optional<User> userOpt = userRepository.findById(userId);
        String name = userOpt.map(User::getName).orElse("Student");
        String email = userOpt.map(User::getEmail).orElse("");

        // 2. Get weak topics
        List<WeakTopicDTO> weakTopics = quizResultRepository.findWeakTopics(userId);

        // 3. Calculate total quizzes and average score
        List<Object[]> stats = quizResultRepository.findQuizStats(userId);
        int totalQuizzes = 0;
        double averageScore = 0.0;
        if (!stats.isEmpty() && stats.get(0) != null) {
            Object[] row = stats.get(0);
            totalQuizzes = row[0] != null ? ((Number) row[0]).intValue() : 0;
            averageScore = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
        }

        // 4. Calculate streak
        int streak = calculateStreak(userId);

        // 5. Generate AI motivation message
        String motivation = generateMotivation(name, streak, weakTopics);

        return new DashboardDTO(name, email, streak, totalQuizzes, averageScore, weakTopics, motivation);
    }

    private int calculateStreak(int userId) {
        List<java.time.LocalDate> dates = studyLogRepository.findStudyDatesByUserId(userId);
        if (dates.isEmpty()) return 0;

        int streak = 0;
        LocalDate today = LocalDate.now();
        for (int i = 0; i < dates.size(); i++) {
            if (dates.get(i).equals(today.minusDays(i))) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private String generateMotivation(String name, int streak, List<WeakTopicDTO> weakTopics) {
        try {
            String weakTopicNames = weakTopics.isEmpty() ? "none" :
                weakTopics.stream()
                    .map(w -> w.getSubject() + " - " + w.getChapter())
                    .reduce("", (a, b) -> a + ", " + b);

            String prompt = """
                Generate a short 1-2 sentence motivational message for a NEET student named %s.
                Their current study streak is %d days.
                Their weak topics are: %s.
                Make it personal, encouraging and specific. Keep it under 50 words.
                """.formatted(name, streak, weakTopicNames);

            return chatClient.prompt().user(prompt).call().content();
        } catch (Exception e) {
            return "Keep pushing " + name + "! Every day of study brings you closer to your NEET goal! 💪";
        }
    }
}