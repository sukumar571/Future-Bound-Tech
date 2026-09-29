package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Full read model for a practice question. Used by the admin manager and the
 * coding-problem detail page. For live MCQ attempts the answer-safe
 * {@link PracticeQuestionTakeDto} is used instead.
 */
@Data
@Accessors(chain = true)
public class PracticeQuestionDto {
    private Long id;
    private String type;
    private String category;
    private String categoryLabel;
    private String topic;
    private String difficulty;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctOption;
    private String explanation;
    private Integer marks;
    private boolean published;
    private String inputFormat;
    private String outputFormat;
    private String constraintsText;
    private String examples;
    private String tags;

    public boolean isCoding() {
        return "CODING".equalsIgnoreCase(type);
    }
}
