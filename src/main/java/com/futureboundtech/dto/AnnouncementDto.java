package com.futureboundtech.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** Announcement read model and create/edit form for the admin portal. */
@Data
@Accessors(chain = true)
public class AnnouncementDto {

    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(max = 2000, message = "Content is too long")
    private String content;

    /** Null batch means a global announcement. */
    private Long batchId;
    private String batchName;
    private boolean global;
    private LocalDateTime createdAt;
}
