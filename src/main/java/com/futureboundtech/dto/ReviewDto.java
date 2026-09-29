package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** Review (testimonial) read model for the admin portal. */
@Data
@Accessors(chain = true)
public class ReviewDto {
    private Long id;
    private String studentName;
    private String courseTitle;
    private String authorName;
    private String contextLabel;
    private Integer rating;
    private String comment;
    private boolean approved;
    private boolean demo;
    private boolean published;
    private String statusLabel;
    private LocalDateTime createdAt;
}
