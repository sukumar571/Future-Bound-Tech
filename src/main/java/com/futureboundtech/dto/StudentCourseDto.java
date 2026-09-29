package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class StudentCourseDto {

    private Long enrollmentId;
    private Long courseId;
    private String title;
    private String slug;
    private String thumbnailUrl;
    private String trainerName;
    private String categoryLabel;
    private String levelLabel;

    private String enrollmentStatus;   // ACTIVE / COMPLETED / DROPPED
    private boolean active;
    private boolean completed;

    /** Human-friendly delivery mode for the enrolled seat (Online / Offline / Hybrid). */
    private String trainingModeLabel;
    /** Soonest scheduled live session for this course, or null when there is none. */
    private java.time.LocalDateTime nextClassTime;
    /** Meeting link for the in-progress / next online class (null for offline or none). */
    private String joinClassUrl;
    /** True when the course has at least one scheduled live session. */
    private boolean hasLiveClass;
    /** Dashboard status badge (Completed / Ongoing / Start here). */
    private String statusLabel;
    /** Bootstrap badge colour name for the status badge (success / primary / info). */
    private String statusVariant;

    private int totalLessons;
    private int completedLessons;
    private int progressPercent;

    private String lastCompletedLesson;
    private String nextLesson;

    private LocalDateTime enrolledAt;
}
