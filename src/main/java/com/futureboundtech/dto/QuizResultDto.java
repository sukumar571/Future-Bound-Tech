package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class QuizResultDto {

    private Long quizId;
    private String quizTitle;
    private int totalQuestions;
    private int correctAnswers;
    private int scorePercent;
    private int passingScore;
    private boolean passed;
    private int attemptNumber;
}
