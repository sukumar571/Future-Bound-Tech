package com.futureboundtech.dto.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/** Enrollment request: joins a course (optionally a specific batch). */
@Data
@Accessors(chain = true)
public class EnrollmentRequest {

    @NotBlank(message = "Course slug is required")
    @Size(max = 200, message = "Course slug is too long")
    private String courseSlug;

    private Long batchId;
}
