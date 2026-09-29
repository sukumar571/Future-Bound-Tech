package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/** Outcome of a single MCQ attempt, revealing the correct option and explanation. */
@Data
@Accessors(chain = true)
public class PracticeQuestionResultDto {
    private Long questionId;
    private String selectedOption;
    private String correctOption;
    private boolean correct;
    private Integer marksAwarded;
    private String explanation;
    private String questionText;

    /** Convenience label for the view. */
    public String getStatusLabel() {
        return correct ? "Correct" : "Incorrect";
    }
}
