package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated trainer dashboard, sourced live from the MySQL database and scoped
 * strictly to the courses and batches assigned to the signed-in trainer.
 */
@Data
@Accessors(chain = true)
public class TrainerStatsDto {

    private TrainerDto profile;

    private long totalCourses;
    private long totalBatches;
    private long totalStudents;
    private long upcomingClasses;
    private long pendingSubmissions;
    private long totalQuizzes;

    private List<CourseDto> courses = new ArrayList<>();
    private List<BatchDto> batches = new ArrayList<>();
    private List<LiveClassDto> upcomingClassList = new ArrayList<>();
    private List<TrainerSubmissionDto> pendingSubmissionList = new ArrayList<>();
    private List<AnnouncementDto> announcements = new ArrayList<>();
}
