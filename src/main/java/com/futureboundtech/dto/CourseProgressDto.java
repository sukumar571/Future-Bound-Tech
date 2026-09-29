package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CourseProgressDto {

    private Long courseId;
    private int totalLessons;
    private int completedLessons;
    private int percent;

    public boolean isStarted() {
        return completedLessons > 0;
    }

    public boolean isAllCompleted() {
        return totalLessons > 0 && completedLessons >= totalLessons;
    }
}
