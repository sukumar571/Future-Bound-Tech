package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** Assignment read model for the admin portal. */
@Data
@Accessors(chain = true)
public class AssignmentDto {
    private Long id;
    private String title;
    private String courseTitle;
    private String batchName;
    private LocalDateTime dueDate;
    private long submissionCount;
    private String dueLabel;
    private LocalDateTime createdAt;
    private Integer maxMarks;
    private String status;        // DRAFT / PUBLISHED
    private boolean published;
    private boolean hasAttachment;
}
