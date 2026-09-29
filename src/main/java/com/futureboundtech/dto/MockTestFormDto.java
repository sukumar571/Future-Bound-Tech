package com.futureboundtech.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/** Admin create/edit form for a mock test. */
@Data
@Accessors(chain = true)
public class MockTestFormDto {

    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    @Min(value = 0, message = "Passing marks cannot be negative")
    private Integer passingMarks;

    private boolean published;

    /** Selected MCQ question ids included in the test. */
    private List<Long> questionIds = new ArrayList<>();
}
