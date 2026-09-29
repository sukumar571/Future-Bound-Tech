package com.futureboundtech.dto.api;

import com.futureboundtech.dto.CourseModuleDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/** Create payload for a syllabus module. */
@Data
@Accessors(chain = true)
public class CourseModuleRequest {

    @NotBlank(message = "Module title is required")
    @Size(max = 180, message = "Module title must be 180 characters or fewer")
    private String title;

    @Size(max = 4000, message = "Module description is too long")
    private String description;

    private Integer orderIndex;

    public CourseModuleDto toDto() {
        return new CourseModuleDto()
                .setTitle(title)
                .setDescription(description)
                .setOrderIndex(orderIndex);
    }
}
