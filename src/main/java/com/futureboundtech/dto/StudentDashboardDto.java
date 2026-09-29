package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class StudentDashboardDto {

    private StudentProfileDto profile;

    private int enrolledCourses;
    private int activeCourses;
    private int completedCourses;
    private int overallProgressPercent;
    private int totalCompletedLessons;
    private int totalPublishedLessons;
    private int upcomingClassesCount;
    private int pendingAssignmentsCount;
    private int certificatesCount;
    private int unreadNotificationsCount;
    private int quizzesTakenCount;
    private int quizzesPassedCount;

    /** Latest payment state summary, e.g. "Paid", "Pending", "No payments yet". */
    private String paymentStatusSummary;
    private String paymentStatusVariant;   // bootstrap color: success / warning / secondary

    /** Next unfinished lesson across the student's active courses (for the Future Mentor). */
    private String nextLessonLabel;

    private List<StudentCourseDto> continueLearning = new ArrayList<>();
    /** Active courses delivered without scheduled live sessions (pure self-study tracks). */
    private List<StudentCourseDto> selfPacedCourses = new ArrayList<>();
    /** Sessions happening now or starting within the next couple of days. */
    private List<StudentClassDto> liveClasses = new ArrayList<>();
    private List<StudentClassDto> upcomingClasses = new ArrayList<>();
    private List<StudentAssignmentDto> pendingAssignments = new ArrayList<>();
    private List<NotificationDto> recentNotifications = new ArrayList<>();
    private List<StudentAnnouncementDto> announcements = new ArrayList<>();
}
