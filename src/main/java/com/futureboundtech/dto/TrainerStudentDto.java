package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Read model for a student enrolled in one of the current trainer's batches.
 * Only students connected to the trainer's own courses/batches are ever exposed.
 */
@Data
@Accessors(chain = true)
public class TrainerStudentDto {
    private Long studentId;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String education;
    private Long batchId;
    private String batchName;
    private Long courseId;
    private String courseTitle;
    private long enrolledCourses;
    private long completedCourses;
    private int progressPercent;
    private long attendancePresent;
    private long attendanceTotal;
    private int attendancePercent;
}
