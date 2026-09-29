package com.futureboundtech.service.impl;

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
import com.futureboundtech.entity.Announcement;
import com.futureboundtech.entity.Assignment;
import com.futureboundtech.entity.Attendance;
import com.futureboundtech.entity.Certificate;
import com.futureboundtech.enums.CertificateStatus;
import com.futureboundtech.entity.Course;
import com.futureboundtech.entity.Enrollment;
import com.futureboundtech.entity.Lesson;
import com.futureboundtech.entity.LessonCompletion;
import com.futureboundtech.entity.LiveClass;
import com.futureboundtech.entity.Payment;
import com.futureboundtech.entity.Quiz;
import com.futureboundtech.entity.QuizAttempt;
import com.futureboundtech.entity.QuizQuestion;
import com.futureboundtech.entity.Student;
import com.futureboundtech.entity.Submission;
import com.futureboundtech.entity.Trainer;
import com.futureboundtech.entity.User;
import com.futureboundtech.enums.ClassStatus;
import com.futureboundtech.enums.EnrollmentStatus;
import com.futureboundtech.enums.PaymentStatus;
import com.futureboundtech.enums.Role;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.exception.ResourceNotFoundException;
import com.futureboundtech.repository.AnnouncementRepository;
import com.futureboundtech.repository.AssignmentRepository;
import com.futureboundtech.repository.AttendanceRepository;
import com.futureboundtech.repository.CertificateRepository;
import com.futureboundtech.repository.EnrollmentRepository;
import com.futureboundtech.repository.LessonCompletionRepository;
import com.futureboundtech.repository.LessonRepository;
import com.futureboundtech.repository.LiveClassRepository;
import com.futureboundtech.repository.PaymentRepository;
import com.futureboundtech.repository.QuizAttemptRepository;
import com.futureboundtech.repository.QuizQuestionRepository;
import com.futureboundtech.repository.QuizRepository;
import com.futureboundtech.repository.StudentRepository;
import com.futureboundtech.repository.SubmissionRepository;
import com.futureboundtech.service.FileStorageService;
import com.futureboundtech.service.NotificationService;
import com.futureboundtech.service.StudentDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentDashboardServiceImpl implements StudentDashboardService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CertificateRepository certificateRepository;
    private final PaymentRepository paymentRepository;
    private final LiveClassRepository liveClassRepository;
    private final AttendanceRepository attendanceRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final AnnouncementRepository announcementRepository;
    private final LessonRepository lessonRepository;
    private final LessonCompletionRepository completionRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    // ================= Dashboard =================

    @Override
    @Transactional(readOnly = true)
    public StudentDashboardDto getDashboard(User user) {
        Student student = requireStudent(user);
        User person = student.getUser();
        List<Enrollment> enrollments = enrollmentRepository.findForStudent(student.getId());

        List<StudentCourseDto> courses = enrollments.stream()
                .filter(StudentDashboardServiceImpl::isPaidSeat)
                .map(e -> toCourseDto(e, student.getId()))
                .collect(Collectors.toList());

        StudentDashboardDto dto = new StudentDashboardDto();
        dto.setProfile(toProfile(student));

        int active = 0;
        int completed = 0;
        int totalPublished = 0;
        int totalCompleted = 0;
        for (StudentCourseDto c : courses) {
            if (c.isActive()) active++;
            if (c.isCompleted()) completed++;
            totalPublished += c.getTotalLessons();
            totalCompleted += c.getCompletedLessons();
        }
        dto.setEnrolledCourses(courses.size());
        dto.setActiveCourses(active);
        dto.setCompletedCourses(completed);
        dto.setTotalPublishedLessons(totalPublished);
        dto.setTotalCompletedLessons(totalCompleted);
        dto.setOverallProgressPercent(totalPublished == 0 ? 0
                : (int) Math.round(totalCompleted * 100.0 / totalPublished));

        // Build the class schedule first so course cards can show class time + a Join link.
        List<StudentClassDto> classes = buildClasses(enrollments);
        enrichCoursesWithClasses(courses, classes);

        List<StudentCourseDto> activeCourses = courses.stream()
                .filter(StudentCourseDto::isActive)
                .sorted(Comparator.comparing(StudentCourseDto::getEnrolledAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        // Continue Learning: cohort/live-track courses the student is actively enrolled in.
        dto.setContinueLearning(activeCourses.stream()
                .filter(StudentCourseDto::isHasLiveClass)
                .limit(6)
                .collect(Collectors.toList()));

        // Self-Paced Courses: active courses delivered without any scheduled live session.
        dto.setSelfPacedCourses(activeCourses.stream()
                .filter(c -> !c.isHasLiveClass())
                .limit(8)
                .collect(Collectors.toList()));

        // Future Mentor nudge: the next unfinished lesson across active courses.
        dto.setNextLessonLabel(activeCourses.stream()
                .map(StudentCourseDto::getNextLesson)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null));

        LocalDateTime liveWindow = LocalDateTime.now().plusHours(48);
        dto.setLiveClasses(classes.stream()
                .filter(c -> c.isInProgress()
                        || (c.isUpcoming() && c.getStartTime() != null && c.getStartTime().isBefore(liveWindow)))
                .sorted(Comparator.comparing(StudentClassDto::getStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(4)
                .collect(Collectors.toList()));

        List<StudentClassDto> upcoming = classes.stream()
                .filter(StudentClassDto::isUpcoming)
                .sorted(Comparator.comparing(StudentClassDto::getStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(4)
                .collect(Collectors.toList());
        dto.setUpcomingClasses(upcoming);
        dto.setUpcomingClassesCount((int) classes.stream().filter(StudentClassDto::isUpcoming).count());

        List<StudentAssignmentDto> assignments = buildAssignments(enrollments, student.getId());
        dto.setPendingAssignments(assignments.stream().filter(a -> !a.isSubmitted()).limit(4).collect(Collectors.toList()));
        dto.setPendingAssignmentsCount((int) assignments.stream().filter(a -> !a.isSubmitted()).count());

        dto.setCertificatesCount((int) certificateRepository.findForStudent(student.getId()).size());

        dto.setUnreadNotificationsCount((int) notificationService.unreadCount(person));
        dto.setRecentNotifications(notificationService.recent(person, 5));

        List<QuizAttempt> attempts = quizAttemptRepository.findByStudent_Id(student.getId());
        dto.setQuizzesTakenCount((int) attempts.stream().map(a -> a.getQuiz().getId()).distinct().count());
        dto.setQuizzesPassedCount((int) attempts.stream().filter(QuizAttempt::isPassed)
                .map(a -> a.getQuiz().getId()).distinct().count());

        applyPaymentSummary(dto, student.getId());

        List<Long> courseIds = enrolledCourseIds(enrollments);
        dto.setAnnouncements(buildAnnouncements(courseIds).stream().limit(3).collect(Collectors.toList()));
        return dto;
    }

    // ================= Profile =================

    @Override
    @Transactional(readOnly = true)
    public StudentProfileDto getProfile(User user) {
        return toProfile(requireStudent(user));
    }

    @Override
    @Transactional
    public void updateProfile(User user, StudentProfileFormDto form) {
        Student student = requireStudent(user);
        String education = form.getEducation() == null ? null : form.getEducation().trim();
        student.setEducation(education == null || education.isEmpty() ? null : education);

        MultipartFile file = form.getPhotoFile();
        if (file != null && !file.isEmpty()) {
            String previous = student.getPhotoUrl();
            student.setPhotoUrl(fileStorageService.storeStudentPhoto(file));
            if (previous != null && previous.startsWith(FileStorageService.AVATAR_PREFIX)) {
                fileStorageService.deleteIfExists(previous);
            }
        }
        studentRepository.save(student);
    }

    /** Turns a stored relative path into the /uploads URL the browser can load. */
    private String resolvePublicUrl(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        String value = stored.trim();
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("/")) {
            return value;
        }
        return "/uploads/" + value.replace("\\", "/");
    }

    // ================= Courses =================

    @Override
    @Transactional(readOnly = true)
    public List<StudentCourseDto> getEnrolledCourses(User user) {
        Student student = requireStudent(user);
        return enrollmentRepository.findForStudent(student.getId()).stream()
                .filter(StudentDashboardServiceImpl::isPaidSeat)
                .map(e -> toCourseDto(e, student.getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentEnrollmentDto> getEnrollmentHistory(User user) {
        Student student = requireStudent(user);
        return enrollmentRepository.findForStudent(student.getId()).stream()
                .map(e -> toEnrollmentHistoryDto(e, student.getId()))
                .collect(Collectors.toList());
    }

    private StudentEnrollmentDto toEnrollmentHistoryDto(Enrollment enrollment, Long studentId) {
        Course course = enrollment.getCourse();
        long total = lessonRepository.countByModule_Course_IdAndPublishedTrue(course.getId());
        long done = completionRepository.countByStudent_IdAndLesson_Module_Course_Id(studentId, course.getId());
        int completedLessons = (int) Math.min(done, total);
        int percent = total == 0 ? 0 : (int) Math.round(completedLessons * 100.0 / total);

        StudentEnrollmentDto dto = new StudentEnrollmentDto()
                .setEnrollmentId(enrollment.getId())
                .setCourseId(course.getId())
                .setCourseTitle(course.getTitle())
                .setCourseSlug(course.getSlug())
                .setThumbnailUrl(resolveThumbnail(course.getThumbnailPath()))
                .setStatus(enrollment.getStatus())
                .setStatusLabel(prettyLabel(enrollment.getStatus()))
                .setAmountLabel(money(enrollment.getAmount()))
                .setPaymentStatusLabel(prettyLabel(enrollment.getPaymentStatus()))
                .setCompletionPercent(enrollment.getStatus() == EnrollmentStatus.COMPLETED ? 100 : percent)
                .setEnrolledAt(enrollment.getCreatedAt());
        if (enrollment.getBatch() != null) {
            dto.setBatchName(enrollment.getBatch().getBatchName());
        }
        dto.setTrainingModeLabel(prettyLabel(enrollment.getTrainingMode()));
        return dto;
    }

    private String prettyLabel(Enum<?> value) {
        if (value == null) {
            return "";
        }
        String[] parts = value.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private String money(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        format.setMaximumFractionDigits(2);
        return format.format(amount.setScale(2, RoundingMode.HALF_UP));
    }

    @Override
    @Transactional(readOnly = true)
    public StudentCourseDto getCourseCard(User user, Long courseId) {
        Student student = requireStudent(user);
        Enrollment enrollment = enrollmentRepository.findByStudent_IdAndCourse_Id(student.getId(), courseId)
                .orElseThrow(() -> new BusinessException("You are not enrolled in this course."));
        return toCourseDto(enrollment, student.getId());
    }

    // ================= Classes =================

    @Override
    @Transactional(readOnly = true)
    public List<StudentClassDto> getClasses(User user) {
        Student student = requireStudent(user);
        List<Enrollment> enrollments = enrollmentRepository.findForStudent(student.getId());
        return buildClasses(enrollments).stream()
                .sorted(Comparator.comparing(StudentClassDto::isUpcoming).reversed()
                        .thenComparing(StudentClassDto::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StudentAttendanceDto getAttendance(User user) {
        Student student = requireStudent(user);
        List<Enrollment> enrollments = enrollmentRepository.findForStudent(student.getId());
        List<Long> courseIds = enrolledCourseIds(enrollments);

        Map<Long, Boolean> marked = attendanceRepository.findByStudent_Id(student.getId()).stream()
                .collect(Collectors.toMap(a -> a.getLiveClass().getId(), Attendance::isPresent, (x, y) -> x));

        StudentAttendanceDto dto = new StudentAttendanceDto();
        long present = 0;
        long marked_ = 0;
        if (!courseIds.isEmpty()) {
            List<LiveClass> classes = liveClassRepository.findAllForCourses(courseIds).stream()
                    .sorted(Comparator.comparing(LiveClass::getStartTime, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
            for (LiveClass lc : classes) {
                StudentAttendanceDto.Row row = new StudentAttendanceDto.Row()
                        .setLiveClassId(lc.getId())
                        .setTopic(lc.getTopic())
                        .setCourseTitle(lc.getBatch().getCourse().getTitle())
                        .setBatchName(lc.getBatch().getBatchName())
                        .setStartTime(lc.getStartTime());
                if (!marked.containsKey(lc.getId())) {
                    row.setStatus("NOT_MARKED").setStatusLabel("Not marked");
                } else {
                    marked_++;
                    boolean isPresent = Boolean.TRUE.equals(marked.get(lc.getId()));
                    if (isPresent) {
                        present++;
                    }
                    row.setStatus(isPresent ? "PRESENT" : "ABSENT")
                            .setStatusLabel(isPresent ? "Present" : "Absent");
                }
                dto.getRows().add(row);
            }
        }
        dto.setTotalClasses(marked_);
        dto.setPresentClasses(present);
        dto.setAttendancePercent(marked_ == 0 ? 0 : (int) Math.round(present * 100.0 / marked_));
        return dto;
    }

    // ================= Assignments =================

    @Override
    @Transactional(readOnly = true)
    public List<StudentAssignmentDto> getAssignments(User user) {
        Student student = requireStudent(user);
        List<Enrollment> enrollments = enrollmentRepository.findForStudent(student.getId());
        return buildAssignments(enrollments, student.getId());
    }

    @Override
    @Transactional
    public void submitAssignment(User user, Long assignmentId, MultipartFile file, String note) {
        Student student = requireStudent(user);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found."));
        requireEnrolledCourse(student.getId(), assignment.getBatch().getCourse().getId());

        if (!assignment.isPublished()) {
            throw new BusinessException("This assignment is not open for submissions.");
        }
        LocalDateTime now = LocalDateTime.now();
        boolean afterDeadline = assignment.getDueDate() != null && assignment.getDueDate().isBefore(now);
        if (afterDeadline && !assignment.isAllowLate()) {
            throw new BusinessException("The submission deadline for this assignment has passed.");
        }

        if (file == null || file.isEmpty()) {
            throw new BusinessException("Choose a file to submit for this assignment.");
        }
        String fileUrl = fileStorageService.storeSubmissionFile(file);

        Optional<Submission> existing = submissionRepository.findByAssignment_IdAndStudent_Id(assignmentId, student.getId());
        Submission submission;
        if (existing.isPresent()) {
            if (existing.get().getStatus().name().equals("GRADED")) {
                throw new BusinessException("This assignment has already been graded and cannot be resubmitted.");
            }
            submission = existing.get();
        } else {
            submission = new Submission();
            submission.setAssignment(assignment);
            submission.setStudent(student);
        }
        submission.setFileUrl(fileUrl);
        submission.setStatus(com.futureboundtech.enums.SubmissionStatus.PENDING);
        submission.setSubmittedAt(now);
        submission.setLate(afterDeadline);
        submissionRepository.save(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public String authorizeAssignmentAttachment(Long assignmentId, User user) {
        Student student = requireStudent(user);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found."));
        requireEnrolledCourse(student.getId(), assignment.getBatch().getCourse().getId());
        if (!assignment.isPublished()) {
            throw new BusinessException("This assignment is not available.");
        }
        String path = assignment.getAttachmentUrl();
        if (path == null || path.isBlank()) {
            throw new ResourceNotFoundException("This assignment has no attachment.");
        }
        return path;
    }

    // ================= Quizzes =================

    @Override
    @Transactional(readOnly = true)
    public List<StudentQuizDto> getQuizzes(User user) {
        Student student = requireStudent(user);
        List<Long> courseIds = enrolledCourseIds(enrollmentRepository.findForStudent(student.getId()));
        if (courseIds.isEmpty()) {
            return List.of();
        }
        Map<Long, List<QuizAttempt>> attemptsByQuiz = quizAttemptRepository.findByStudent_Id(student.getId()).stream()
                .collect(Collectors.groupingBy(a -> a.getQuiz().getId()));

        return quizRepository.findForCourses(courseIds).stream()
                .filter(Quiz::isPublished)
                .map(quiz -> toQuizDto(quiz, attemptsByQuiz.getOrDefault(quiz.getId(), List.of())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StudentQuizTakeDto getQuizToTake(User user, Long quizId) {
        Student student = requireStudent(user);
        Quiz quiz = requireAccessibleQuiz(student.getId(), quizId);
        StudentQuizTakeDto dto = new StudentQuizTakeDto()
                .setQuizId(quiz.getId())
                .setTitle(quiz.getTitle())
                .setCourseTitle(quiz.getCourse().getTitle())
                .setTimeLimitMinutes(quiz.getTimeLimitMinutes() == null ? 0 : quiz.getTimeLimitMinutes())
                .setPassingScore(quiz.getPassingScore() == null ? 0 : quiz.getPassingScore());
        List<StudentQuizTakeDto.Question> questions = quizQuestionRepository.findByQuiz_Id(quizId).stream()
                .map(this::toTakeQuestion)
                .collect(Collectors.toList());
        dto.setQuestions(questions);
        return dto;
    }

    @Override
    @Transactional
    public QuizResultDto submitQuiz(User user, Long quizId, Map<Long, String> answers) {
        Student student = requireStudent(user);
        Quiz quiz = requireAccessibleQuiz(student.getId(), quizId);
        List<QuizQuestion> questions = quizQuestionRepository.findByQuiz_Id(quizId);
        if (questions.isEmpty()) {
            throw new BusinessException("This quiz has no questions yet.");
        }
        int correct = 0;
        for (QuizQuestion q : questions) {
            String answer = answers.get(q.getId());
            if (answer != null && q.getCorrectOption() != null
                    && answer.trim().equalsIgnoreCase(q.getCorrectOption().trim())) {
                correct++;
            }
        }
        int total = questions.size();
        int percent = (int) Math.round(correct * 100.0 / total);
        int threshold = quiz.getPassingScore() == null ? 0 : quiz.getPassingScore();
        boolean passed = percent >= threshold;

        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .student(student)
                .scoreObtained(percent)
                .isPassed(passed)
                .build();
        quizAttemptRepository.save(attempt);

        int attemptNumber = (int) quizAttemptRepository.countByQuiz_IdAndStudent_Id(quizId, student.getId());

        // The result also lands in the inbox so the student sees it without reopening the quiz.
        notificationService.quizResult(user, quiz.getTitle(), percent, passed, correct, total, quizId);

        return new QuizResultDto()
                .setQuizId(quizId)
                .setQuizTitle(quiz.getTitle())
                .setTotalQuestions(total)
                .setCorrectAnswers(correct)
                .setScorePercent(percent)
                .setPassingScore(threshold)
                .setPassed(passed)
                .setAttemptNumber(attemptNumber);
    }

    // ================= Payments / Certificates =================

    @Override
    @Transactional(readOnly = true)
    public List<StudentPaymentDto> getPayments(User user) {
        Student student = requireStudent(user);
        return paymentRepository.findByStudent_IdOrderByCreatedAtDesc(student.getId()).stream()
                .map(this::toPaymentDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentCertificateDto> getCertificates(User user) {
        Student student = requireStudent(user);
        return certificateRepository.findForStudent(student.getId()).stream()
                .map(this::toCertificateDto)
                .collect(Collectors.toList());
    }

    // ================= Announcements =================
    // The inbox itself lives in NotificationService; controllers read it directly.

    @Override
    @Transactional(readOnly = true)
    public List<StudentAnnouncementDto> getAnnouncements(User user) {
        Student student = requireStudent(user);
        List<Long> courseIds = enrolledCourseIds(enrollmentRepository.findForStudent(student.getId()));
        return buildAnnouncements(courseIds);
    }

    // ================= Build helpers =================

    private List<StudentClassDto> buildClasses(List<Enrollment> enrollments) {
        List<Long> courseIds = enrolledCourseIds(enrollments);
        if (courseIds.isEmpty()) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now();
        return liveClassRepository.findAllForCourses(courseIds).stream()
                .map(lc -> toClassDto(lc, now))
                .collect(Collectors.toList());
    }

    private List<StudentAssignmentDto> buildAssignments(List<Enrollment> enrollments, Long studentId) {
        List<Long> courseIds = enrolledCourseIds(enrollments);
        if (courseIds.isEmpty()) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now();
        return assignmentRepository.findForCourses(courseIds).stream()
                .filter(Assignment::isPublished)
                .map(a -> toAssignmentDto(a, studentId, now))
                .collect(Collectors.toList());
    }

    private List<StudentAnnouncementDto> buildAnnouncements(List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return announcementRepository.findByBatchIsNullOrderByCreatedAtDesc().stream()
                    .map(this::toAnnouncementDto)
                    .collect(Collectors.toList());
        }
        return announcementRepository.findForStudent(courseIds).stream()
                .map(this::toAnnouncementDto)
                .collect(Collectors.toList());
    }

    private StudentCourseDto toCourseDto(Enrollment enrollment, Long studentId) {
        Course course = enrollment.getCourse();
        StudentCourseDto dto = new StudentCourseDto()
                .setEnrollmentId(enrollment.getId())
                .setCourseId(course.getId())
                .setTitle(course.getTitle())
                .setSlug(course.getSlug())
                .setThumbnailUrl(resolveThumbnail(course.getThumbnailPath()))
                .setTrainerName(trainerName(course))
                .setCategoryLabel(course.getCategory() == null ? "" : course.getCategory().getLabel())
                .setLevelLabel(capitalize(course.getLevel() == null ? null : course.getLevel().name()))
                .setTrainingModeLabel(deliveryModeLabel(enrollment))
                .setEnrollmentStatus(enrollment.getStatus() == null ? "" : enrollment.getStatus().name())
                .setEnrolledAt(enrollment.getCreatedAt());

        long total = lessonRepository.countByModule_Course_IdAndPublishedTrue(course.getId());
        long done = completionRepository.countByStudent_IdAndLesson_Module_Course_Id(studentId, course.getId());
        int completedLessons = (int) Math.min(done, total);
        int percent = total == 0 ? 0 : (int) Math.round(completedLessons * 100.0 / total);
        dto.setTotalLessons((int) total)
                .setCompletedLessons(completedLessons)
                .setProgressPercent(percent);

        boolean isCompleted = enrollment.getStatus() == EnrollmentStatus.COMPLETED || (total > 0 && percent >= 100);
        dto.setActive(enrollment.getStatus() == EnrollmentStatus.ACTIVE && !isCompleted)
                .setCompleted(isCompleted);

        if (isCompleted) {
            dto.setStatusLabel("Completed").setStatusVariant("success");
        } else if (percent > 0) {
            dto.setStatusLabel("Ongoing").setStatusVariant("primary");
        } else {
            dto.setStatusLabel("Start here").setStatusVariant("info");
        }

        applyLessonPointers(dto, course.getId(), studentId);
        return dto;
    }

    /** Delivery mode shown on the dashboard card: fall back to the batch mode, then Online. */
    private String deliveryModeLabel(Enrollment enrollment) {
        var mode = enrollment.getTrainingMode();
        if (mode == null && enrollment.getBatch() != null) {
            mode = enrollment.getBatch().getMode();
        }
        return mode == null ? "Online" : prettyLabel(mode);
    }

    /**
     * Attach each course card its soonest joinable live session (class time + meeting link).
     * In-progress sessions win over merely-scheduled ones so a running class surfaces a Join button.
     */
    private void enrichCoursesWithClasses(List<StudentCourseDto> courses, List<StudentClassDto> classes) {
        Map<Long, StudentClassDto> nextByCourse = new java.util.HashMap<>();
        for (StudentClassDto c : classes) {
            boolean relevant = c.isInProgress() || c.isUpcoming();
            if (!relevant || c.getStartTime() == null) {
                continue;
            }
            StudentClassDto current = nextByCourse.get(c.getCourseId());
            boolean replace = current == null
                    || (c.isInProgress() && !current.isInProgress())
                    || (c.isInProgress() == current.isInProgress()
                        && c.getStartTime().isBefore(current.getStartTime()));
            if (replace) {
                nextByCourse.put(c.getCourseId(), c);
            }
        }
        for (StudentCourseDto course : courses) {
            StudentClassDto next = nextByCourse.get(course.getCourseId());
            if (next != null) {
                course.setHasLiveClass(true)
                        .setNextClassTime(next.getStartTime())
                        .setJoinClassUrl(next.getMeetingLink());
            }
        }
    }

    private void applyLessonPointers(StudentCourseDto dto, Long courseId, Long studentId) {
        List<LessonCompletion> completions = completionRepository
                .findByStudent_IdAndLesson_Module_Course_Id(studentId, courseId);
        Set<Long> completedIds = completions.stream()
                .map(c -> c.getLesson().getId())
                .collect(Collectors.toSet());
        completions.stream()
                .filter(c -> c.getCompletedAt() != null)
                .max(Comparator.comparing(LessonCompletion::getCompletedAt))
                .ifPresent(c -> dto.setLastCompletedLesson(c.getLesson().getTitle()));

        lessonRepository.findByModule_Course_IdAndPublishedTrueOrderByModule_OrderIndexAscOrderIndexAsc(courseId).stream()
                .filter(l -> !completedIds.contains(l.getId()))
                .findFirst()
                .ifPresent(l -> dto.setNextLesson(l.getTitle()));
    }

    private StudentClassDto toClassDto(LiveClass lc, LocalDateTime now) {
        boolean upcoming = lc.getStartTime() != null && !lc.getStartTime().isBefore(now)
                && lc.getStatus() != ClassStatus.CANCELLED && lc.getStatus() != ClassStatus.COMPLETED;
        // A class is happening now when it is explicitly IN_PROGRESS, or its start/end
        // window currently encloses "now" (and it was neither cancelled nor completed).
        boolean inProgress = lc.getStatus() == ClassStatus.IN_PROGRESS
                || (lc.getStartTime() != null && !lc.getStartTime().isAfter(now)
                    && lc.getEndTime() != null && lc.getEndTime().isAfter(now)
                    && lc.getStatus() != ClassStatus.CANCELLED && lc.getStatus() != ClassStatus.COMPLETED);
        Course course = lc.getBatch().getCourse();
        com.futureboundtech.enums.ClassMode mode = lc.getMode() == null
                ? com.futureboundtech.enums.ClassMode.ONLINE : lc.getMode();
        boolean offline = mode == com.futureboundtech.enums.ClassMode.OFFLINE;
        // Meeting link is only revealed for classes the (authorized, enrolled) student can attend live.
        String meetingLink = (!offline && (upcoming || inProgress)) ? lc.getMeetingLink() : null;
        String classroom = offline
                ? (lc.getClassroomDetails() != null ? lc.getClassroomDetails() : lc.getBatch().getClassroomAddress())
                : null;
        return new StudentClassDto()
                .setId(lc.getId())
                .setTopic(lc.getTopic())
                .setCourseId(course.getId())
                .setCourseTitle(course.getTitle())
                .setBatchName(lc.getBatch().getBatchName())
                .setStartTime(lc.getStartTime())
                .setEndTime(lc.getEndTime())
                .setMode(mode.name())
                .setModeLabel(mode.getLabel())
                .setMeetingLink(meetingLink)
                .setClassroomDetails(classroom)
                .setStatus(lc.getStatus() == null ? "" : lc.getStatus().name())
                .setUpcoming(upcoming)
                .setInProgress(inProgress);
    }

    private StudentAssignmentDto toAssignmentDto(Assignment a, Long studentId, LocalDateTime now) {
        Submission submission = submissionRepository
                .findByAssignment_IdAndStudent_Id(a.getId(), studentId).orElse(null);
        boolean submitted = submission != null;
        boolean overdue = a.getDueDate() != null && a.getDueDate().isBefore(now) && !submitted;
        Course course = a.getBatch().getCourse();
        return new StudentAssignmentDto()
                .setId(a.getId())
                .setTitle(a.getTitle())
                .setDescription(a.getDescription())
                .setCourseId(course.getId())
                .setCourseTitle(course.getTitle())
                .setDueDate(a.getDueDate())
                .setOverdue(overdue)
                .setMaxMarks(a.getMaxMarks())
                .setAllowLate(a.isAllowLate())
                .setHasAttachment(a.getAttachmentUrl() != null && !a.getAttachmentUrl().isBlank())
                .setAttachmentName(a.getAttachmentName())
                .setSubmitted(submitted)
                .setSubmissionStatus(submission == null ? null : submission.getStatus().name())
                .setScore(submission == null ? null : submission.getScore())
                .setFeedback(submission == null ? null : submission.getFeedback())
                .setLate(submission != null && submission.isLate())
                .setSubmittedAt(submission == null ? null : submission.getSubmittedAt());
    }

    private StudentQuizDto toQuizDto(Quiz quiz, List<QuizAttempt> attempts) {
        Integer best = attempts.stream()
                .map(QuizAttempt::getScoreObtained)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);
        boolean passed = attempts.stream().anyMatch(QuizAttempt::isPassed);
        return new StudentQuizDto()
                .setId(quiz.getId())
                .setTitle(quiz.getTitle())
                .setCourseId(quiz.getCourse().getId())
                .setCourseTitle(quiz.getCourse().getTitle())
                .setQuestionCount((int) quizQuestionRepository.countByQuiz_Id(quiz.getId()))
                .setTimeLimitMinutes(quiz.getTimeLimitMinutes() == null ? 0 : quiz.getTimeLimitMinutes())
                .setPassingScore(quiz.getPassingScore() == null ? 0 : quiz.getPassingScore())
                .setAttemptCount(attempts.size())
                .setAttemptLimit(quiz.getAttemptLimit())
                .setBestScore(best)
                .setPassed(passed);
    }

    private StudentQuizTakeDto.Question toTakeQuestion(QuizQuestion q) {
        List<StudentQuizTakeDto.Option> options = new ArrayList<>();
        addOption(options, "A", q.getOptionA());
        addOption(options, "B", q.getOptionB());
        addOption(options, "C", q.getOptionC());
        addOption(options, "D", q.getOptionD());
        return new StudentQuizTakeDto.Question()
                .setId(q.getId())
                .setQuestionText(q.getQuestionText())
                .setOptions(options);
    }

    private void addOption(List<StudentQuizTakeDto.Option> options, String key, String text) {
        if (text != null && !text.isBlank()) {
            options.add(new StudentQuizTakeDto.Option(key, text));
        }
    }

    private StudentPaymentDto toPaymentDto(Payment p) {
        return new StudentPaymentDto()
                .setId(p.getId())
                .setCourseTitle(p.getCourse().getTitle())
                .setCourseId(p.getCourse().getId())
                .setReceiptNumber(p.getReceiptNumber())
                .setAmount(p.getAmount())
                .setDiscountAmount(p.getDiscountAmount())
                .setCurrency(p.getCurrency())
                .setStatus(p.getStatus() == null ? "" : p.getStatus().name())
                .setPaymentMethod(p.getPaymentMethod())
                .setPaidAt(p.getPaidAt())
                .setCreatedAt(p.getCreatedAt());
    }

    private StudentCertificateDto toCertificateDto(Certificate c) {
        return new StudentCertificateDto()
                .setId(c.getId())
                .setCertificateNumber(c.getCertificateNumber())
                .setCourseId(c.getEnrollment().getCourse().getId())
                .setCourseTitle(c.getEnrollment().getCourse().getTitle())
                .setIssueDate(c.getIssueDate())
                .setCertificateUrl(c.getCertificateUrl())
                .setStatusLabel(prettyLabel(c.getStatus()))
                .setIssued(c.getStatus() == CertificateStatus.ISSUED)
                .setPendingApproval(c.getStatus() == CertificateStatus.PENDING_APPROVAL)
                .setRevoked(c.getStatus() == CertificateStatus.REVOKED);
    }

    private StudentAnnouncementDto toAnnouncementDto(Announcement a) {
        String scope = a.getBatch() == null ? "Institute-wide" : a.getBatch().getBatchName();
        return new StudentAnnouncementDto()
                .setId(a.getId())
                .setTitle(a.getTitle())
                .setContent(a.getContent())
                .setScope(scope)
                .setCreatedAt(a.getCreatedAt());
    }

    private StudentProfileDto toProfile(Student student) {
        User user = student.getUser();
        String first = user.getFirstName() == null ? "" : user.getFirstName();
        String last = user.getLastName() == null ? "" : user.getLastName();
        String full = (first + " " + last).trim();
        String initials = initials(first, last);
        return new StudentProfileDto()
                .setStudentId(student.getId())
                .setUserId(user.getId())
                .setFirstName(first)
                .setLastName(last)
                .setFullName(full.isEmpty() ? user.getEmail() : full)
                .setInitials(initials)
                .setEmail(user.getEmail())
                .setPhone(user.getPhone())
                .setEducation(student.getEducation())
                .setAvatarUrl(resolvePublicUrl(student.getPhotoUrl()))
                .setMemberSince(user.getCreatedAt());
    }

    private void applyPaymentSummary(StudentDashboardDto dto, Long studentId) {
        List<Payment> payments = paymentRepository.findByStudent_IdOrderByCreatedAtDesc(studentId);
        if (payments.isEmpty()) {
            dto.setPaymentStatusSummary("No payments yet");
            dto.setPaymentStatusVariant("secondary");
            return;
        }
        boolean anySettled = payments.stream().anyMatch(Payment::isSettled);
        boolean anyPending = payments.stream().anyMatch(p -> p.getStatus() == PaymentStatus.PENDING || p.getStatus() == PaymentStatus.CREATED);
        boolean anyFailed = payments.stream().anyMatch(p -> p.getStatus() == PaymentStatus.FAILED || p.getStatus() == PaymentStatus.CANCELLED);
        if (anySettled) {
            dto.setPaymentStatusSummary("Paid");
            dto.setPaymentStatusVariant("success");
        } else if (anyPending) {
            dto.setPaymentStatusSummary("Payment pending");
            dto.setPaymentStatusVariant("warning");
        } else if (anyFailed) {
            dto.setPaymentStatusSummary("Payment failed");
            dto.setPaymentStatusVariant("danger");
        } else {
            dto.setPaymentStatusSummary("No payments yet");
            dto.setPaymentStatusVariant("secondary");
        }
    }

    // ================= Access & utility =================

    private void requireEnrolledCourse(Long studentId, Long courseId) {
        // SECURITY (Phase 25): assignments / quizzes require a PAID enrollment, matching
        // lesson access. PENDING_PAYMENT seats are reserved before any money is captured.
        boolean paid = enrollmentRepository.existsByStudent_IdAndCourse_IdAndStatusIn(
                studentId, courseId, java.util.List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED));
        if (!paid) {
            throw new BusinessException("You are not enrolled in this course.");
        }
    }

    private Quiz requireAccessibleQuiz(Long studentId, Long quizId) {
        Quiz quiz = quizRepository.findWithCourse(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found."));
        if (!quiz.isPublished()) {
            throw new BusinessException("This quiz is not available yet.");
        }
        requireEnrolledCourse(studentId, quiz.getCourse().getId());
        if (quiz.getAttemptLimit() != null) {
            long taken = quizAttemptRepository.countByQuiz_IdAndStudent_Id(quizId, studentId);
            if (taken >= quiz.getAttemptLimit()) {
                throw new BusinessException("You have used all " + quiz.getAttemptLimit()
                        + (quiz.getAttemptLimit() == 1 ? " attempt" : " attempts") + " allowed for this quiz.");
            }
        }
        return quiz;
    }

    private Student requireStudent(User user) {
        if (user == null || user.getRole() != Role.STUDENT) {
            throw new BusinessException("Student access only.");
        }
        return studentRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new BusinessException("Student profile is missing. Please contact support."));
    }

    private List<Long> enrolledCourseIds(List<Enrollment> enrollments) {
        return enrollments.stream()
                .filter(StudentDashboardServiceImpl::isPaidSeat)
                .map(e -> e.getCourse().getId())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /** Only enrollments backed by a completed payment grant access to course content. */
    private static boolean isPaidSeat(Enrollment enrollment) {
        EnrollmentStatus status = enrollment.getStatus();
        return status == EnrollmentStatus.ACTIVE || status == EnrollmentStatus.COMPLETED;
    }

    private String resolveThumbnail(String path) {
        return path == null || path.isBlank() ? null : "/uploads/" + path;
    }

    private String trainerName(Course course) {
        Trainer trainer = course.getTrainer();
        if (trainer == null || trainer.getUser() == null) {
            return "To be assigned";
        }
        User u = trainer.getUser();
        String name = ((u.getFirstName() == null ? "" : u.getFirstName()) + " "
                + (u.getLastName() == null ? "" : u.getLastName())).trim();
        return name.isEmpty() ? u.getEmail() : name;
    }

    private String initials(String first, String last) {
        StringBuilder sb = new StringBuilder();
        if (first != null && !first.isBlank()) sb.append(Character.toUpperCase(first.charAt(0)));
        if (last != null && !last.isBlank()) sb.append(Character.toUpperCase(last.charAt(0)));
        return sb.length() == 0 ? "ST" : sb.toString();
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
