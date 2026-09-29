package com.futureboundtech.service;

import com.futureboundtech.entity.Assignment;
import com.futureboundtech.entity.Enrollment;
import com.futureboundtech.entity.LiveClass;
import com.futureboundtech.enums.ClassStatus;
import com.futureboundtech.enums.NotificationRelatedType;
import com.futureboundtech.enums.NotificationType;
import com.futureboundtech.entity.User;
import com.futureboundtech.repository.AssignmentRepository;
import com.futureboundtech.repository.EnrollmentRepository;
import com.futureboundtech.repository.LiveClassRepository;
import com.futureboundtech.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Background sweep that turns upcoming classes and approaching deadlines into
 * reminders.
 *
 * <p>Every reminder goes through {@link NotificationService#notifyOnce}, which
 * records the triggering entity, so the hourly job can never repeat a reminder
 * for the same class or assignment to the same student.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationReminderScheduler {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    private final LiveClassRepository liveClassRepository;
    private final AssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SubmissionRepository submissionRepository;
    private final NotificationService notificationService;

    @Value("${app.notifications.reminders.enabled:true}")
    private boolean enabled;

    /** How far ahead of a live class the reminder is worth sending. */
    @Value("${app.notifications.reminders.class-lead-minutes:120}")
    private long classLeadMinutes;

    /** How far ahead of an assignment deadline the reminder is worth sending. */
    @Value("${app.notifications.reminders.assignment-lead-hours:24}")
    private long assignmentLeadHours;

    @Scheduled(cron = "${app.notifications.reminders.cron:0 0 * * * *}")
    public void scheduledSweep() {
        if (!enabled) {
            return;
        }
        runSweep();
    }

    /**
     * One pass over classes and assignments. Returns the number of reminders
     * created — also called directly by the admin console so the job can be
     * exercised without waiting for the next cron tick.
     */
    @Transactional
    public int runSweep() {
        int sent = 0;
        sent += classReminders();
        sent += assignmentDeadlineReminders();
        if (sent > 0) {
            log.info("Notification reminder sweep created {} reminder(s)", sent);
        }
        return sent;
    }

    private int classReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.plusMinutes(Math.max(classLeadMinutes, 0));
        int sent = 0;
        for (LiveClass liveClass : liveClassRepository.findStartingBetween(now, cutoff)) {
            if (liveClass.getStatus() == ClassStatus.CANCELLED || liveClass.getBatch() == null) {
                continue;
            }
            Long classId = liveClass.getId();
            String when = liveClass.getStartTime() == null ? "shortly" : liveClass.getStartTime().format(WHEN);
            String title = "Class reminder: " + liveClass.getTopic();
            String message = "\"" + liveClass.getTopic() + "\" (" + liveClass.getBatch().getBatchName()
                    + ") starts at " + when + ". Don't be late.";
            for (Enrollment enrollment : enrollmentRepository.findByBatch_IdOrderByCreatedAtDesc(liveClass.getBatch().getId())) {
                User student = userOf(enrollment);
                if (student == null) {
                    continue;
                }
                if (notificationService.notifyOnce(student, NotificationType.CLASS_REMINDER,
                        title, message, NotificationRelatedType.LIVE_CLASS, classId)) {
                    sent++;
                }
            }
        }
        return sent;
    }

    private int assignmentDeadlineReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.plusHours(Math.max(assignmentLeadHours, 0));
        int sent = 0;
        for (Assignment assignment : assignmentRepository.findByDueDateBetweenOrderByDueDateAsc(now, cutoff)) {
            if (!assignment.isPublished() || assignment.getBatch() == null) {
                continue;
            }
            Long assignmentId = assignment.getId();
            String due = assignment.getDueDate() == null ? "soon" : assignment.getDueDate().format(WHEN);
            String title = "Assignment due: " + assignment.getTitle();
            String message = "\"" + assignment.getTitle() + "\" (" + assignment.getBatch().getBatchName()
                    + ") is due " + due + ". Submit it before the deadline.";
            for (Enrollment enrollment : enrollmentRepository.findByBatch_IdOrderByCreatedAtDesc(assignment.getBatch().getId())) {
                User student = userOf(enrollment);
                if (student == null || student.getStudentProfile() == null) {
                    continue;
                }
                if (submissionRepository.existsByAssignment_IdAndStudent_Id(
                        assignmentId, student.getStudentProfile().getId())) {
                    continue;
                }
                if (notificationService.notifyOnce(student, NotificationType.ASSIGNMENT_DEADLINE,
                        title, message, NotificationRelatedType.ASSIGNMENT, assignmentId)) {
                    sent++;
                }
            }
        }
        return sent;
    }

    private User userOf(Enrollment enrollment) {
        if (enrollment == null || enrollment.getStudent() == null) {
            return null;
        }
        User user = enrollment.getStudent().getUser();
        return user != null && user.isActive() ? user : null;
    }
}
