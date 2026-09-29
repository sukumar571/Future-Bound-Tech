package com.futureboundtech.service;

import com.futureboundtech.dto.QuizResultDto;
import com.futureboundtech.dto.StudentAnnouncementDto;
import com.futureboundtech.dto.StudentAssignmentDto;
import com.futureboundtech.dto.StudentAttendanceDto;
import com.futureboundtech.dto.StudentCertificateDto;
import com.futureboundtech.dto.StudentClassDto;
import com.futureboundtech.dto.StudentCourseDto;
import com.futureboundtech.dto.StudentDashboardDto;
import com.futureboundtech.dto.StudentEnrollmentDto;
import com.futureboundtech.dto.StudentPaymentDto;
import com.futureboundtech.dto.StudentProfileDto;
import com.futureboundtech.dto.StudentProfileFormDto;
import com.futureboundtech.dto.StudentQuizDto;
import com.futureboundtech.dto.StudentQuizTakeDto;
import com.futureboundtech.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Aggregates every student-facing dashboard view. Every method is scoped to the
 * given (authenticated) student so a student can only ever see their own data.
 */
public interface StudentDashboardService {

    StudentDashboardDto getDashboard(User user);

    StudentProfileDto getProfile(User user);

    /** Updates the student's own profile (optional photo upload + editable text fields). */
    void updateProfile(User user, StudentProfileFormDto form);

    List<StudentCourseDto> getEnrolledCourses(User user);

    /** Full enrollment history (including pending/cancelled/expired) for the student. */
    List<StudentEnrollmentDto> getEnrollmentHistory(User user);

    StudentCourseDto getCourseCard(User user, Long courseId);

    List<StudentClassDto> getClasses(User user);

    StudentAttendanceDto getAttendance(User user);

    List<StudentAssignmentDto> getAssignments(User user);

    void submitAssignment(User user, Long assignmentId, MultipartFile file, String note);

    /**
     * Authorises a student to download an assignment's attachment. Returns the stored
     * relative path only when the student is enrolled, the assignment is published and
     * an attachment exists; otherwise a business/not-found exception is thrown.
     */
    String authorizeAssignmentAttachment(Long assignmentId, User user);

    List<StudentQuizDto> getQuizzes(User user);

    StudentQuizTakeDto getQuizToTake(User user, Long quizId);

    QuizResultDto submitQuiz(User user, Long quizId, Map<Long, String> answers);

    List<StudentPaymentDto> getPayments(User user);

    List<StudentCertificateDto> getCertificates(User user);

    /**
     * Announcements visible to this student: institute-wide posts plus anything
     * scoped to a course they are enrolled in. The inbox itself is served by
     * {@link NotificationService}.
     */
    List<StudentAnnouncementDto> getAnnouncements(User user);
}
