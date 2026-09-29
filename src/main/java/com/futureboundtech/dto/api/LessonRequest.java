package com.futureboundtech.dto.api;

import com.futureboundtech.dto.LessonDto;
import com.futureboundtech.enums.LessonType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/** Create/update payload for a lesson within a module. */
@Data
@Accessors(chain = true)
public class LessonRequest {

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

    public LessonDto toDto() {
        return new LessonDto()
                .setTitle(title)
                .setDescription(description)
                .setLessonType(lessonType)
                .setVideoUrl(videoUrl)
                .setNotes(notes)
                .setDurationMinutes(durationMinutes)
                .setOrderIndex(orderIndex)
                .setPublished(published);
    }
}
