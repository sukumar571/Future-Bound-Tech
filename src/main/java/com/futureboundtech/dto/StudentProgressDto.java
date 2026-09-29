package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** Per-course progress row shown on the trainer's student-detail page. */
@Data
@Accessors(chain = true)
public class StudentProgressDto {
    private Long enrollmentId;
    private Long courseId;
    private String courseTitle;
    private String batchName;
    private long totalLessons;
    private long completedLessons;
    private int percent;
    private String status;
    private String statusLabel;
    private LocalDateTime enrolledAt;
}
