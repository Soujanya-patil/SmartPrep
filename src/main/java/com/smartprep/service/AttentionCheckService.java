package com.smartprep.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartprep.dto.AttentionQuestionDTO;
import com.smartprep.model.QuizQuestion;
import com.smartprep.repository.QuizQuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Serves the multiple-choice attention checks shown while a student watches a study video.
 * <p>
 * Questions come from the quiz_questions cache. The AI is called only when a chapter has nothing
 * cached, through {@link AIQuizService}, whose single call saves 10 questions, so later checks
 * for that chapter cost no AI quota.
 */
@Service
public class AttentionCheckService {

    private static final Logger log = LoggerFactory.getLogger(AttentionCheckService.class);

    static final String QUOTA_MESSAGE = "Attention questions are unavailable right now (AI daily limit reached).";
    static final String FAILURE_MESSAGE = "Couldn't load an attention question for this topic right now.";
    static final String MISSING_CHAPTER_MESSAGE = "A chapter or topic is needed for an attention check.";

    private static final int HINT_MAX_LENGTH = 200;
    private static final int OPTION_COUNT = 4;

    /** Subjects that mean "not a real subject" (search results arrive as "General"). */
    private static final Set<String> GENERIC_SUBJECTS = Set.of("", "general", "all");
    private static final String GENERIC_SUBJECT = "General";

    private static final List<String> ENCOURAGEMENTS = List.of(
            "Great focus! Keep going 🔥",
            "Nailed it! You're paying attention 💪",
            "Sharp as ever! On to the next part 🚀",
            "Correct! Your focus is paying off ⭐",
            "Well done! Keep this streak alive 🎯"
    );

    private final QuizQuestionRepository quizRepo;
    private final AIQuizService aiQuizService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AttentionCheckService(QuizQuestionRepository quizRepo, AIQuizService aiQuizService) {
        this.quizRepo = quizRepo;
        this.aiQuizService = aiQuizService;
    }

    /**
     * Returns a random attention-check question for a subject and chapter.
     * <ol>
     *   <li>A cached question for the chapter. For a "General" or blank subject (search results),
     *       any cached question whose chapter contains the text, in any subject.</li>
     *   <li>If none, one AI call generates and saves 10 questions for the chapter.</li>
     *   <li>If that fails, a cached question from the same subject (any chapter).</li>
     * </ol>
     *
     * @param subject the video's subject; "General" or blank means unknown
     * @param chapter the video's chapter, or the text the student searched for
     * @return a question with all fields filled in
     * @throws AttentionCheckException if no question can be served; quota-related when the AI limit was the cause
     */
    public AttentionQuestionDTO getAttentionQuestion(String subject, String chapter) {
        String cleanSubject = subject == null ? "" : subject.trim();
        String cleanChapter = chapter == null ? "" : chapter.trim();
        if (cleanChapter.isEmpty()) {
            throw new AttentionCheckException(MISSING_CHAPTER_MESSAGE, false);
        }
        boolean genericSubject = GENERIC_SUBJECTS.contains(cleanSubject.toLowerCase(Locale.ROOT));

        // 1. Cached questions for this chapter
        List<QuizQuestion> cached = genericSubject
                ? quizRepo.findByChapterContainingIgnoreCase(escapeLike(cleanChapter))
                : quizRepo.findBySubjectIgnoreCaseAndChapterIgnoreCase(cleanSubject, cleanChapter);
        Optional<AttentionQuestionDTO> picked = pickRandom(cached);
        if (picked.isPresent()) {
            log.info("Attention check for '{}' / '{}' served from cache ({} cached rows)",
                    cleanSubject, cleanChapter, cached.size());
            return picked.get();
        }

        // 2. Nothing usable cached: one AI call creates and saves 10 questions for this chapter
        String aiSubject = genericSubject ? GENERIC_SUBJECT : cleanSubject;
        Exception aiFailure = null;
        try {
            picked = pickRandom(aiQuizService.generateAndSaveQuestions(aiSubject, cleanChapter));
            if (picked.isPresent()) {
                log.info("Attention check for '{}' / '{}' served from newly generated questions", aiSubject, cleanChapter);
                return picked.get();
            }
            log.warn("AI returned no usable attention questions for '{}' / '{}'", aiSubject, cleanChapter);
        } catch (Exception e) {
            aiFailure = e;
            log.warn("AI question generation failed for '{}' / '{}': {}", aiSubject, cleanChapter, abbreviate(e.getMessage()));
        }

        // 3. Fall back to any cached question from the same subject
        if (!genericSubject) {
            picked = pickRandom(quizRepo.findBySubjectIgnoreCase(cleanSubject));
            if (picked.isPresent()) {
                log.info("Attention check for '{}' / '{}' fell back to another {} chapter", cleanSubject, cleanChapter, cleanSubject);
                return picked.get();
            }
        }

        // 4. Nothing to show
        if (aiFailure != null && isQuotaError(aiFailure)) {
            throw new AttentionCheckException(QUOTA_MESSAGE, true);
        }
        throw new AttentionCheckException(FAILURE_MESSAGE, false);
    }

    /** Tries the candidates in random order and returns the first one that converts cleanly. */
    private Optional<AttentionQuestionDTO> pickRandom(List<QuizQuestion> candidates) {
        List<QuizQuestion> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled);
        for (QuizQuestion row : shuffled) {
            Optional<AttentionQuestionDTO> converted = toAttentionQuestion(row);
            if (converted.isPresent()) return converted;
        }
        return Optional.empty();
    }

    /**
     * Converts a cached quiz row. Skips rows without a question, without exactly 4 non-blank
     * options, or whose correctAnswer matches none of the options.
     */
    private Optional<AttentionQuestionDTO> toAttentionQuestion(QuizQuestion row) {
        String question = trimToEmpty(row.getQuestionText());
        if (question.isEmpty()) return Optional.empty();

        List<String> options;
        try {
            options = objectMapper.readValue(row.getOptions(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Optional.empty();
        }
        if (options == null || options.size() != OPTION_COUNT) return Optional.empty();
        options = options.stream().map(AttentionCheckService::trimToEmpty).toList();
        if (options.stream().anyMatch(String::isEmpty)) return Optional.empty();

        String correctAnswer = trimToEmpty(row.getCorrectAnswer());
        int correctIndex = -1;
        for (int i = 0; i < OPTION_COUNT && !correctAnswer.isEmpty(); i++) {
            if (options.get(i).equalsIgnoreCase(correctAnswer)) {
                correctIndex = i;
                break;
            }
        }
        if (correctIndex < 0) return Optional.empty();

        return Optional.of(new AttentionQuestionDTO(
                question,
                options.get(0),
                options.get(1),
                options.get(2),
                options.get(3),
                String.valueOf((char) ('A' + correctIndex)),
                ENCOURAGEMENTS.get(ThreadLocalRandom.current().nextInt(ENCOURAGEMENTS.size())),
                buildHint(row)
        ));
    }

    private static String buildHint(QuizQuestion row) {
        String explanation = trimToEmpty(row.getExplanation());
        if (!explanation.isEmpty()) return shorten(explanation, HINT_MAX_LENGTH);
        String chapter = trimToEmpty(row.getChapter());
        return chapter.isEmpty()
                ? "Think back to what the video just covered 💪"
                : "Think back to what the video covered on " + chapter + " 💪";
    }

    /** Cuts text to about maxLength characters at a word boundary, adding an ellipsis. */
    static String shorten(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        int cut = text.lastIndexOf(' ', maxLength);
        if (cut < maxLength / 2) cut = maxLength; // one very long word: hard cut
        return text.substring(0, cut).stripTrailing() + "…";
    }

    /** True if the failure was the AI provider's rate limit / daily quota (HTTP 429, RESOURCE_EXHAUSTED). */
    static boolean isQuotaError(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof RestClientResponseException e && e.getStatusCode().value() == 429) return true;
            if (t instanceof WebClientResponseException e && e.getStatusCode().value() == 429) return true;
            String message = t.getMessage();
            if (message != null) {
                String lower = message.toLowerCase(Locale.ROOT);
                if (lower.contains("429") || lower.contains("resource_exhausted") || lower.contains("quota")) return true;
            }
        }
        return false;
    }

    /** Escapes LIKE wildcards with '!' so a typed '%' or '_' is matched literally. */
    private static String escapeLike(String text) {
        return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private static String abbreviate(String message) {
        if (message == null) return "";
        return message.length() <= 300 ? message : message.substring(0, 300) + "…";
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
