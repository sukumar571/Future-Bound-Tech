package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/** An MCQ presented to a student for an attempt. Deliberately omits the answer. */
@Data
@Accessors(chain = true)
public class PracticeQuestionTakeDto {
    private Long id;
    private String category;
    private String categoryLabel;
    private String topic;
    private String difficulty;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private Integer marks;
}
