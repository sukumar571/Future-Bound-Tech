package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** A student's assignment submission as reviewed by the trainer. */
@Data
@Accessors(chain = true)
public class TrainerSubmissionDto {
    private Long submissionId;
    private Long assignmentId;
    private Long studentId;
    private String assignmentTitle;
    private String courseTitle;
    private String batchName;
    private String studentName;
    private String studentEmail;
    private String fileUrl;
    private String status;
    private String statusLabel;
    private Integer score;
    private String feedback;
    private LocalDateTime dueDate;
    private LocalDateTime submittedAt;
    private Integer maxMarks;
    private boolean late;
}
