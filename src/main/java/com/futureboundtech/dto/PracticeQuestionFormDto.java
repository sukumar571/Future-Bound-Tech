package com.futureboundtech.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

/** Admin create/edit form for a practice question (MCQ or CODING). */
@Data
@Accessors(chain = true)
public class PracticeQuestionFormDto {

    private Long id;

    /** MCQ or CODING. */
    private String type;

    /** One of QuestionCategory names. */
    private String category;

    private String topic;

    /** EASY / MEDIUM / HARD. */
    private String difficulty;

    @NotBlank(message = "Question / problem statement is required")
    private String questionText;

    // ----- MCQ -----
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctOption;
    private String explanation;

    @Min(value = 0, message = "Marks cannot be negative")
    private Integer marks;

    private boolean published;

    // ----- CODING -----
    private String inputFormat;
    private String outputFormat;
    private String constraintsText;
    private String examples;
    private String tags;

    public boolean isCoding() {
        return "CODING".equalsIgnoreCase(trimmed(type));
    }

    private static String trimmed(String s) {
        return s == null ? null : s.trim();
    }
}
