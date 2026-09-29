package com.futureboundtech.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** FAQ item read/write model for the admin console and public page. */
@Data
@Accessors(chain = true)
public class FaqDto {

    private Long id;

    @NotBlank(message = "Question is required")
    @Size(max = 300, message = "Question is too long")
    private String question;

    @NotBlank(message = "Answer is required")
    @Size(max = 2000, message = "Answer is too long")
    private String answer;

    @Size(max = 60, message = "Category is too long")
    private String category;

    private boolean published = true;

    private Integer sortOrder = 0;

    private LocalDateTime createdAt;
}
