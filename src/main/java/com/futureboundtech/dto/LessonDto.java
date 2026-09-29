package com.futureboundtech.dto;

import com.futureboundtech.enums.LessonType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class LessonDto {

    private Long id;

    @NotBlank(message = "Lesson title is required")
    @Size(max = 180, message = "Lesson title must be 180 characters or fewer")
    private String title;

    @Size(max = 8000, message = "Lesson description is too long")
    private String description;

    @NotNull(message = "Lesson type is required")
    private LessonType lessonType;

    @Size(max = 500, message = "Video URL must be 500 characters or fewer")
    private String videoUrl;

    @Size(max = 8000, message = "Lesson notes are too long")
    private String notes;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 1440, message = "Duration cannot exceed 1440 minutes")
    private Integer durationMinutes;

    private Integer orderIndex;

    private boolean published = false;

    private Long moduleId;
    private String moduleTitle;
    private Integer moduleOrderIndex;

    private Long courseId;
    private String courseSlug;
    private String courseTitle;

    private List<LessonResourceDto> resources = new ArrayList<>();

    // View helpers
    private String videoEmbedUrl;
    private String durationLabel;
    private boolean completed;

    public boolean hasVideo() {
        return videoUrl != null && !videoUrl.isBlank();
    }
}
