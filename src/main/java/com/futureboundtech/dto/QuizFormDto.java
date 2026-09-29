package com.futureboundtech.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/** Create/edit form for a trainer quiz. */
@Data
@Accessors(chain = true)
public class QuizFormDto {

    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Course is required")
    private Long courseId;

    private String courseTitle;

    @NotNull(message = "Time limit is required")
    @Min(value = 1, message = "Time limit must be at least 1 minute")
    private Integer timeLimitMinutes;

    @NotNull(message = "Passing score is required")
    @Min(value = 0, message = "Passing score cannot be negative")
    private Integer passingScore;

    @Min(value = 1, message = "Attempt limit must be at least 1")
    private Integer attemptLimit;

    /** DRAFT or PUBLISHED. */
    private String status;

    private long questionCount;

    public boolean isPublished() {
        return status == null || status.isBlank() || "PUBLISHED".equalsIgnoreCase(status.trim());
    }
}
