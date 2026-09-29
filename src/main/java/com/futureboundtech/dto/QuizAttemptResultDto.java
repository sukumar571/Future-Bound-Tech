package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** A student's quiz attempt shown on the trainer's results page. */
@Data
@Accessors(chain = true)
public class QuizAttemptResultDto {
    private Long attemptId;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private Integer scoreObtained;
    private boolean passed;
    private String passedLabel;
    private LocalDateTime attemptedAt;
}
