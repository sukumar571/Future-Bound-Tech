package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** Quiz read model for the admin portal. */
@Data
@Accessors(chain = true)
public class QuizDto {
    private Long id;
    private String title;
    private String courseTitle;
    private long questionCount;
    private Integer timeLimitMinutes;
    private Integer passingScore;
    private long attemptCount;
    private LocalDateTime createdAt;
    private Integer attemptLimit;
    private String status;        // DRAFT / PUBLISHED
    private boolean published;
}
