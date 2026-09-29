package com.futureboundtech.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/** Create/edit form for a trainer assignment. */
@Data
@Accessors(chain = true)
public class AssignmentFormDto {

    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Batch is required")
    private Long batchId;

    private String courseTitle;

    @NotNull(message = "Due date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dueDate;

    @Min(value = 0, message = "Maximum marks cannot be negative")
    private Integer maxMarks;

    /** DRAFT or PUBLISHED. */
    private String status;

    /** Allow submissions after the deadline. */
    private boolean allowLate;

    /** Existing attachment filename (edit view). Cleared automatically if a new file is uploaded. */
    private String attachmentName;

    /** New attachment upload (optional). */
    private MultipartFile attachmentFile;

    public boolean isPublished() {
        return status == null || status.isBlank() || "PUBLISHED".equalsIgnoreCase(status.trim());
    }
}
