package com.smartprep.dto;

/**
 * One multiple-choice attention-check question shown as a popup while a student watches a video.
 * Every field is a non-empty string; correctOption is "A", "B", "C" or "D".
 */
public record AttentionQuestionDTO(
        String question,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        String correctOption,
        String encouragement,
        String hint
) {
}
