package com.futureboundtech.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class CourseModuleDto {

    private Long id;

    @NotBlank(message = "Module title is required")
    @Size(max = 180, message = "Module title must be 180 characters or fewer")
    private String title;

    @Size(max = 4000, message = "Module description is too long")
    private String description;

    private Integer orderIndex;

    private Long courseId;
    private String courseTitle;
    private String courseSlug;

    private List<LessonDto> lessons = new ArrayList<>();

    private int lessonCount;
    private int publishedLessonCount;
    private int completedLessonCount;
    private int progressPercent;
}
