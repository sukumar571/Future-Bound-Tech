package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class StudentAssignmentDto {

    private Long id;
    private String title;
    private String description;
    private Long courseId;
    private String courseTitle;
    private LocalDateTime dueDate;
    private boolean overdue;
    private Integer maxMarks;
    private boolean allowLate;
    private boolean hasAttachment;
    private String attachmentName;

    private boolean submitted;
    private String submissionStatus;   // null / PENDING / GRADED / REJECTED
    private Integer score;
    private String feedback;
    private boolean late;
    private LocalDateTime submittedAt;

    /** True when submissions are closed (deadline passed and late not allowed) and nothing is graded yet. */
    public boolean isClosed() {
        return overdue && !allowLate;
    }
}
