package com.futureboundtech.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class LessonResourceDto {

    private Long id;

    @NotBlank(message = "Resource title is required")
    @Size(max = 200, message = "Resource title must be 200 characters or fewer")
    private String title;

    @NotBlank(message = "Resource type is required")
    private String resourceType;

    @Size(max = 1000, message = "External link must be 1000 characters or fewer")
    private String fileUrl;

    private boolean storedFile;
    private Long lessonId;
    private String lessonTitle;
}
