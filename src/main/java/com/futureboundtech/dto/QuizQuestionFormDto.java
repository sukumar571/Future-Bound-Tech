package com.futureboundtech.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

/** Create/edit form for a single quiz question. */
@Data
@Accessors(chain = true)
public class QuizQuestionFormDto {

    private Long id;

    private Long quizId;

    @NotBlank(message = "Question text is required")
    private String questionText;

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    @NotBlank(message = "Select the correct option")
    private String correctOption;
}
