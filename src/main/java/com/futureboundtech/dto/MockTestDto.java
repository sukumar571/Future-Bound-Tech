package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/** Read model for a mock test in admin/student listings. */
@Data
@Accessors(chain = true)
public class MockTestDto {
    private Long id;
    private String title;
    private String description;
    private Integer durationMinutes;
    private Integer passingMarks;
    private boolean published;
    private int questionCount;
    private int totalMarks;
    private int attemptCount;
    private Long bestScore;

    /** Attempts taken by the current student (student view only). */
    private int myAttempts;
    private Integer myBestPercent;
}
