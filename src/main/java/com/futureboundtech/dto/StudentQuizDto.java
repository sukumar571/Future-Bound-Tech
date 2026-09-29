package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class StudentQuizDto {

    private Long id;
    private String title;
    private Long courseId;
    private String courseTitle;
    private int questionCount;
    private int timeLimitMinutes;
    private int passingScore;

    private int attemptCount;
    private Integer bestScore;
    private boolean passed;
    private Integer attemptLimit;

    /** True when a limit is set and all attempts are used up. */
    public boolean isAttemptsExhausted() {
        return attemptLimit != null && attemptCount >= attemptLimit;
    }

    /** Remaining attempts, or null when unlimited. */
    public Integer getAttemptsRemaining() {
        return attemptLimit == null ? null : Math.max(0, attemptLimit - attemptCount);
    }
}
