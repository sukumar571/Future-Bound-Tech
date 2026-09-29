package com.futureboundtech.service.impl;

import com.futureboundtech.dto.*;
import com.futureboundtech.entity.*;
import com.futureboundtech.enums.*;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.exception.ResourceNotFoundException;
import com.futureboundtech.repository.*;
import com.futureboundtech.service.CourseService;
import com.futureboundtech.service.FileStorageService;
import com.futureboundtech.service.NotificationService;
import com.futureboundtech.service.TrainerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainerServiceImpl implements TrainerService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a");

    private final TrainerRepository trainerRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final BatchRepository batchRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final LiveClassRepository liveClassRepository;
    private final AttendanceRepository attendanceRepository;
    private final AnnouncementRepository announcementRepository;
    private final NotificationService notificationService;
    private final LessonRepository lessonRepository;
    private final LessonCompletionRepository lessonCompletionRepository;
    private final CourseService courseService;
    private final FileStorageService fileStorageService;

    // =====================================================================
    // Dashboard
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public TrainerStatsDto dashboard(User user) {
        Trainer trainer = requireTrainer(user);
        List<Course> courses = ownCourses(trainer);
        List<Batch> batches = ownBatches(trainer);
        List<Long> batchIds = ids(batches, Batch::getId);
        List<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());
        LocalDateTime now = LocalDateTime.now();

        TrainerStatsDto stats = new TrainerStatsDto()
                .setProfile(toTrainerDto(trainer))
                .setTotalCourses(courses.size())
                .setTotalBatches(batches.size());

        // Distinct students across the trainer's batches.
        List<Enrollment> enrollments = batchIds.isEmpty()
                ? Collections.emptyList()
                : enrollmentRepository.findByBatch_IdInOrderByCreatedAtDesc(batchIds);
        stats.setTotalStudents(enrollments.stream()
                .map(e -> e.getStudent().getId()).distinct().count());

        List<LiveClass> upcoming = courseIds.isEmpty()
                ? Collections.<LiveClass>emptyList()
                : liveClassRepository.findUpcomingForCourses(courseIds, now);
        stats.setUpcomingClasses(upcoming.size());
        stats.setUpcomingClassList(upcoming.stream().limit(6).map(this::toLiveClassDto).collect(Collectors.toList()));

        stats.setAnnouncements(batchIds.isEmpty()
                ? Collections.<AnnouncementDto>emptyList()
                : announcementRepository.findByBatch_IdInOrderByCreatedAtDesc(batchIds).stream()
                        .limit(5).map(this::toAnnouncementDto).collect(Collectors.toList()));

        stats.setTotalQuizzes(courseIds.isEmpty() ? 0 : quizRepository.findForCourses(courseIds).size());

        List<Assignment> assignments = batchIds.isEmpty()
                ? Collections.<Assignment>emptyList()
                : assignmentRepository.findByBatch_IdInOrderByDueDateAsc(batchIds);
        stats.setPendingSubmissions(submissionsForAssignments(assignments).stream()
                .filter(s -> s.getStatus() == SubmissionStatus.PENDING).count());
        stats.setPendingSubmissionList(recentPendingSubmissions(assignments));

        stats.setCourses(courseService.findCoursesForTrainer(user));
        stats.setBatches(batches.stream().map(this::toBatchDto).collect(Collectors.toList()));
        return stats;
    }

    // =====================================================================
    // Profile
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public TrainerProfileFormDto getProfile(User user) {
        Trainer trainer = requireTrainer(user);
        User u = trainer.getUser();
        return new TrainerProfileFormDto()
                .setFirstName(u.getFirstName())
                .setLastName(u.getLastName())
                .setPhone(u.getPhone())
                .setEmail(u.getEmail())
                .setExpertise(trainer.getExpertise())
                .setBio(trainer.getBio());
    }

    @Override
    @Transactional
    public void updateProfile(User user, TrainerProfileFormDto form) {
        Trainer trainer = requireTrainer(user);
        User u = trainer.getUser();
        u.setFirstName(requireTrimmed(form.getFirstName(), "First name is required."));
        u.setLastName(requireTrimmed(form.getLastName(), "Last name is required."));
        u.setPhone(trimToNull(form.getPhone()));
        userRepository.save(u);
        trainer.setExpertise(trimToNull(form.getExpertise()));
        trainer.setBio(trimToNull(form.getBio()));
        trainerRepository.save(trainer);
    }

    // =====================================================================
    // Batches
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<BatchDto> listBatches(User user) {
        return ownBatches(requireTrainer(user)).stream()
                .map(this::toBatchDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BatchDto getBatch(User user, Long batchId) {
        return toBatchDto(requireOwnedBatch(requireTrainer(user), batchId));
    }

    // =====================================================================
    // Students
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<TrainerStudentDto> listStudents(User user, Long batchId, String query) {
        Trainer trainer = requireTrainer(user);
        List<Batch> batches = ownBatches(trainer);
        if (batchId != null) {
            batches = batches.stream().filter(b -> b.getId().equals(batchId)).collect(Collectors.toList());
        }
        String q = query == null ? null : query.trim().toLowerCase(Locale.ROOT);

        List<TrainerStudentDto> rows = new ArrayList<>();
        for (Batch batch : batches) {
            long totalLessons = lessonRepository.countByModule_Course_IdAndPublishedTrue(batch.getCourse().getId());
            for (Enrollment e : enrollmentRepository.findByBatch_IdOrderByCreatedAtDesc(batch.getId())) {
                Student student = e.getStudent();
                User su = student.getUser();
                if (q != null && !q.isEmpty()
                        && !fullName(su).toLowerCase(Locale.ROOT).contains(q)
                        && (su.getEmail() == null || !su.getEmail().toLowerCase(Locale.ROOT).contains(q))) {
                    continue;
                }
                long completed = lessonCompletionRepository
                        .countByStudent_IdAndLesson_Module_Course_Id(student.getId(), batch.getCourse().getId());
                long[] att = attendanceStats(student.getId());
                rows.add(new TrainerStudentDto()
                        .setStudentId(student.getId())
                        .setUserId(su.getId())
                        .setFullName(fullName(su))
                        .setEmail(su.getEmail())
                        .setPhone(su.getPhone())
                        .setEducation(student.getEducation())
                        .setBatchId(batch.getId())
                        .setBatchName(batch.getBatchName())
                        .setCourseId(batch.getCourse().getId())
                        .setCourseTitle(batch.getCourse().getTitle())
                        .setProgressPercent(percent(completed, totalLessons))
                        .setAttendancePresent(att[0])
                        .setAttendanceTotal(att[1])
                        .setAttendancePercent(percent(att[0], att[1])));
            }
        }
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public TrainerStudentDto getStudent(User user, Long studentId) {
        Trainer trainer = requireTrainer(user);
        List<Enrollment> relevant = trainerEnrollments(trainer).stream()
                .filter(e -> e.getStudent().getId().equals(studentId))
                .collect(Collectors.toList());
        if (relevant.isEmpty()) {
            throw new AccessDeniedException("This student is not enrolled in any of your batches.");
        }
        Student student = relevant.get(0).getStudent();
        User su = student.getUser();
        long totalLessons = relevant.stream()
                .mapToLong(e -> lessonRepository.countByModule_Course_IdAndPublishedTrue(e.getCourse().getId())).sum();
        long completedLessons = relevant.stream()
                .mapToLong(e -> lessonCompletionRepository.countByStudent_IdAndLesson_Module_Course_Id(student.getId(), e.getCourse().getId())).sum();
        long[] att = attendanceStats(student.getId());
        return new TrainerStudentDto()
                .setStudentId(student.getId())
                .setUserId(su.getId())
                .setFullName(fullName(su))
                .setEmail(su.getEmail())
                .setPhone(su.getPhone())
                .setEducation(student.getEducation())
                .setEnrolledCourses(relevant.size())
                .setCompletedCourses(relevant.stream().filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED).count())
                .setProgressPercent(percent(completedLessons, totalLessons))
                .setAttendancePresent(att[0])
                .setAttendanceTotal(att[1])
                .setAttendancePercent(percent(att[0], att[1]));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentProgressDto> studentProgress(User user, Long studentId) {
        Trainer trainer = requireTrainer(user);
        List<Enrollment> relevant = trainerEnrollments(trainer).stream()
                .filter(e -> e.getStudent().getId().equals(studentId))
                .collect(Collectors.toList());
        if (relevant.isEmpty()) {
            throw new AccessDeniedException("This student is not enrolled in any of your batches.");
        }
        List<StudentProgressDto> rows = new ArrayList<>();
        for (Enrollment e : relevant) {
            long total = lessonRepository.countByModule_Course_IdAndPublishedTrue(e.getCourse().getId());
            long done = lessonCompletionRepository.countByStudent_IdAndLesson_Module_Course_Id(studentId, e.getCourse().getId());
            rows.add(new StudentProgressDto()
                    .setEnrollmentId(e.getId())
                    .setCourseId(e.getCourse().getId())
                    .setCourseTitle(e.getCourse().getTitle())
                    .setBatchName(e.getBatch() == null ? null : e.getBatch().getBatchName())
                    .setTotalLessons(total)
                    .setCompletedLessons(done)
                    .setPercent(percent(done, total))
                    .setStatus(e.getStatus().name())
                    .setStatusLabel(label(e.getStatus()))
                    .setEnrolledAt(e.getCreatedAt()));
        }
        return rows;
    }

    // =====================================================================
    // Assignments
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<AssignmentDto> listAssignments(User user) {
        Trainer trainer = requireTrainer(user);
        return assignmentRepository.findByBatch_IdInOrderByDueDateAsc(ids(ownBatches(trainer), Batch::getId)).stream()
                .map(a -> new AssignmentDto()
                        .setId(a.getId())
                        .setTitle(a.getTitle())
                        .setCourseTitle(a.getBatch().getCourse().getTitle())
                        .setBatchName(a.getBatch().getBatchName())
                        .setDueDate(a.getDueDate())
                        .setMaxMarks(a.getMaxMarks())
                        .setStatus(a.getStatus() == null ? null : a.getStatus().name())
                        .setPublished(a.isPublished())
                        .setHasAttachment(a.getAttachmentUrl() != null && !a.getAttachmentUrl().isBlank())
                        .setSubmissionCount(submissionRepository.countByAssignment_Id(a.getId()))
                        .setDueLabel(a.getDueDate() == null ? null : a.getDueDate().format(DATE_TIME))
                        .setCreatedAt(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentFormDto getAssignment(User user, Long id) {
        Assignment a = requireOwnedAssignment(requireTrainer(user), id);
        return new AssignmentFormDto()
                .setId(a.getId())
                .setTitle(a.getTitle())
                .setDescription(a.getDescription())
                .setBatchId(a.getBatch().getId())
                .setCourseTitle(a.getBatch().getCourse().getTitle())
                .setDueDate(a.getDueDate())
                .setMaxMarks(a.getMaxMarks())
                .setStatus(a.getStatus() == null ? null : a.getStatus().name())
                .setAllowLate(a.isAllowLate())
                .setAttachmentName(a.getAttachmentName());
    }

    @Override
    @Transactional
    public void createAssignment(User user, AssignmentFormDto form) {
        Trainer trainer = requireTrainer(user);
        Batch batch = requireOwnedBatch(trainer, form.getBatchId());
        Assignment a = new Assignment();
        a.setTitle(requireTrimmed(form.getTitle(), "Title is required."));
        a.setDescription(trimToNull(form.getDescription()));
        a.setBatch(batch);
        a.setDueDate(form.getDueDate());
        applyAssignmentFields(a, form);
        assignmentRepository.save(a);
        storeAssignmentAttachment(a, form);
    }

    @Override
    @Transactional
    public void updateAssignment(User user, Long id, AssignmentFormDto form) {
        Trainer trainer = requireTrainer(user);
        Assignment a = requireOwnedAssignment(trainer, id);
        Batch batch = requireOwnedBatch(trainer, form.getBatchId());
        a.setTitle(requireTrimmed(form.getTitle(), "Title is required."));
        a.setDescription(trimToNull(form.getDescription()));
        a.setBatch(batch);
        a.setDueDate(form.getDueDate());
        applyAssignmentFields(a, form);
        assignmentRepository.save(a);
        storeAssignmentAttachment(a, form);
    }

    /** Copies the scalar assignment fields and persists the (optional) new attachment. */
    private void applyAssignmentFields(Assignment a, AssignmentFormDto form) {
        a.setMaxMarks(form.getMaxMarks());
        a.setAllowLate(form.isAllowLate());
        a.setStatus(parseAssignmentStatus(form.getStatus()));
    }

    private void storeAssignmentAttachment(Assignment a, AssignmentFormDto form) {
        MultipartFile file = form.getAttachmentFile();
        if (file == null || file.isEmpty()) {
            return;
        }
        String path = fileStorageService.storeAssignmentAttachment(file);
        // Replace the previous attachment, if any.
        if (a.getAttachmentUrl() != null) {
            fileStorageService.deleteIfExists(a.getAttachmentUrl());
        }
        a.setAttachmentUrl(path);
        a.setAttachmentName(trimToNull(file.getOriginalFilename()));
        assignmentRepository.save(a);
    }

    private AssignmentStatus parseAssignmentStatus(String status) {
        if (status == null || status.isBlank()) {
            return AssignmentStatus.PUBLISHED;
        }
        try {
            return AssignmentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            throw new BusinessException("Unknown assignment status.");
        }
    }

    @Override
    @Transactional
    public void deleteAssignment(User user, Long id) {
        Assignment a = requireOwnedAssignment(requireTrainer(user), id);
        assignmentRepository.delete(a);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainerSubmissionDto> listSubmissions(User user, Long assignmentId) {
        Assignment a = requireOwnedAssignment(requireTrainer(user), assignmentId);
        return submissionRepository.findByAssignment_IdOrderByCreatedAtDesc(a.getId()).stream()
                .map(s -> toSubmissionDto(s, a)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void gradeSubmission(User user, Long submissionId, Integer score, String feedback, String status) {
        Trainer trainer = requireTrainer(user);
        Submission s = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found."));
        assertOwnedAssignmentEntity(trainer, s.getAssignment());
        if (score != null && score < 0) {
            throw new BusinessException("Score cannot be negative.");
        }
        Integer maxMarks = s.getAssignment().getMaxMarks();
        if (score != null && maxMarks != null && score > maxMarks) {
            throw new BusinessException("Score cannot exceed the maximum marks (" + maxMarks + ") for this assignment.");
        }
        s.setScore(score);
        s.setFeedback(trimToNull(feedback));
        s.setStatus(parseSubmissionStatus(status, score));
        submissionRepository.save(s);
    }

    @Override
    @Transactional(readOnly = true)
    public TrainerSubmissionDto getSubmissionForDownload(User user, Long submissionId) {
        Trainer trainer = requireTrainer(user);
        Submission s = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found."));
        assertOwnedAssignmentEntity(trainer, s.getAssignment());
        return toSubmissionDto(s, s.getAssignment());
    }

    // =====================================================================
    // Quizzes
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<QuizDto> listQuizzes(User user) {
        Trainer trainer = requireTrainer(user);
        List<Long> courseIds = ownCourses(trainer).stream().map(Course::getId).collect(Collectors.toList());
        if (courseIds.isEmpty()) {
            return Collections.emptyList();
        }
        return quizRepository.findForCourses(courseIds).stream()
                .map(q -> new QuizDto()
                        .setId(q.getId())
                        .setTitle(q.getTitle())
                        .setCourseTitle(q.getCourse().getTitle())
                        .setQuestionCount(quizQuestionRepository.countByQuiz_Id(q.getId()))
                        .setTimeLimitMinutes(q.getTimeLimitMinutes())
                        .setPassingScore(q.getPassingScore())
                        .setAttemptLimit(q.getAttemptLimit())
                        .setStatus(q.getStatus() == null ? null : q.getStatus().name())
                        .setPublished(q.isPublished())
                        .setAttemptCount(quizAttemptRepository.countByQuiz_Id(q.getId()))
                        .setCreatedAt(q.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public QuizFormDto getQuiz(User user, Long id) {
        Quiz q = requireOwnedQuiz(requireTrainer(user), id);
        return new QuizFormDto()
                .setId(q.getId())
                .setTitle(q.getTitle())
                .setCourseId(q.getCourse().getId())
                .setCourseTitle(q.getCourse().getTitle())
                .setTimeLimitMinutes(q.getTimeLimitMinutes())
                .setPassingScore(q.getPassingScore())
                .setAttemptLimit(q.getAttemptLimit())
                .setStatus(q.getStatus() == null ? null : q.getStatus().name())
                .setQuestionCount(quizQuestionRepository.countByQuiz_Id(q.getId()));
    }

    @Override
    @Transactional
    public void createQuiz(User user, QuizFormDto form) {
        Trainer trainer = requireTrainer(user);
        Course course = requireOwnedCourse(trainer, form.getCourseId());
        Quiz q = new Quiz();
        q.setTitle(requireTrimmed(form.getTitle(), "Title is required."));
        q.setCourse(course);
        q.setTimeLimitMinutes(form.getTimeLimitMinutes());
        q.setPassingScore(form.getPassingScore());
        q.setAttemptLimit(form.getAttemptLimit());
        q.setStatus(parseQuizStatus(form.getStatus()));
        quizRepository.save(q);
    }

    @Override
    @Transactional
    public void updateQuiz(User user, Long id, QuizFormDto form) {
        Trainer trainer = requireTrainer(user);
        Quiz q = requireOwnedQuiz(trainer, id);
        Course course = requireOwnedCourse(trainer, form.getCourseId());
        long questionCount = quizQuestionRepository.countByQuiz_Id(id);
        if (questionCount > 0 && form.getPassingScore() != null && form.getPassingScore() > questionCount) {
            throw new BusinessException("Passing score cannot exceed the total number of questions (" + questionCount + ").");
        }
        q.setTitle(requireTrimmed(form.getTitle(), "Title is required."));
        q.setCourse(course);
        q.setTimeLimitMinutes(form.getTimeLimitMinutes());
        q.setPassingScore(form.getPassingScore());
        q.setAttemptLimit(form.getAttemptLimit());
        q.setStatus(parseQuizStatus(form.getStatus()));
        quizRepository.save(q);
    }

    @Override
    @Transactional
    public void toggleQuizPublished(User user, Long id, boolean published) {
        Quiz q = requireOwnedQuiz(requireTrainer(user), id);
        q.setStatus(published ? QuizStatus.PUBLISHED : QuizStatus.DRAFT);
        quizRepository.save(q);
    }

    private QuizStatus parseQuizStatus(String status) {
        if (status == null || status.isBlank()) {
            return QuizStatus.PUBLISHED;
        }
        try {
            return QuizStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            throw new BusinessException("Unknown quiz status.");
        }
    }

    @Override
    @Transactional
    public void deleteQuiz(User user, Long id) {
        Quiz q = requireOwnedQuiz(requireTrainer(user), id);
        quizRepository.delete(q);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizQuestionFormDto> listQuestions(User user, Long quizId) {
        requireOwnedQuiz(requireTrainer(user), quizId);
        return quizQuestionRepository.findByQuiz_Id(quizId).stream().map(this::toQuestionDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public QuizQuestionFormDto getQuestion(User user, Long questionId) {
        Trainer trainer = requireTrainer(user);
        QuizQuestion question = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found."));
        requireOwnedQuiz(trainer, question.getQuiz().getId());
        return toQuestionDto(question);
    }

    @Override
    @Transactional
    public void addQuestion(User user, Long quizId, QuizQuestionFormDto form) {
        Quiz quiz = requireOwnedQuiz(requireTrainer(user), quizId);
        QuizQuestion question = new QuizQuestion();
        question.setQuiz(quiz);
        applyQuestion(question, form);
        quizQuestionRepository.save(question);
    }

    @Override
    @Transactional
    public void updateQuestion(User user, Long questionId, QuizQuestionFormDto form) {
        Trainer trainer = requireTrainer(user);
        QuizQuestion question = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found."));
        requireOwnedQuiz(trainer, question.getQuiz().getId());
        applyQuestion(question, form);
        quizQuestionRepository.save(question);
    }

    @Override
    @Transactional
    public void deleteQuestion(User user, Long questionId) {
        Trainer trainer = requireTrainer(user);
        QuizQuestion question = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found."));
        requireOwnedQuiz(trainer, question.getQuiz().getId());
        quizQuestionRepository.delete(question);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizAttemptResultDto> quizResults(User user, Long quizId) {
        requireOwnedQuiz(requireTrainer(user), quizId);
        return quizAttemptRepository.findByQuiz_IdOrderByCreatedAtDesc(quizId).stream()
                .map(a -> {
                    User su = a.getStudent().getUser();
                    return new QuizAttemptResultDto()
                            .setAttemptId(a.getId())
                            .setStudentId(a.getStudent().getId())
                            .setStudentName(fullName(su))
                            .setStudentEmail(su.getEmail())
                            .setScoreObtained(a.getScoreObtained())
                            .setPassed(a.isPassed())
                            .setPassedLabel(a.isPassed() ? "Passed" : "Failed")
                            .setAttemptedAt(a.getCreatedAt());
                })
                .collect(Collectors.toList());
    }

    // =====================================================================
    // Classes
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<LiveClassDto> listClasses(User user) {
        Trainer trainer = requireTrainer(user);
        List<Long> batchIds = ids(ownBatches(trainer), Batch::getId);
        if (batchIds.isEmpty()) {
            return Collections.emptyList();
        }
        return liveClassRepository.findAllForBatches(batchIds).stream()
                .map(this::toLiveClassDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TrainerClassFormDto getClass(User user, Long id) {
        LiveClass lc = requireOwnedClass(requireTrainer(user), id);
        return new TrainerClassFormDto()
                .setId(lc.getId())
                .setTopic(lc.getTopic())
                .setBatchId(lc.getBatch().getId())
                .setBatchName(lc.getBatch().getBatchName())
                .setCourseTitle(lc.getBatch().getCourse().getTitle())
                .setStartTime(lc.getStartTime())
                .setEndTime(lc.getEndTime())
                .setMode(lc.getMode() == null ? ClassMode.ONLINE : lc.getMode())
                .setMeetingLink(lc.getMeetingLink())
                .setClassroomDetails(lc.getClassroomDetails())
                .setStatus(lc.getStatus() == null ? ClassStatus.SCHEDULED : lc.getStatus());
    }

    @Override
    @Transactional
    public void createClass(User user, TrainerClassFormDto form) {
        Trainer trainer = requireTrainer(user);
        Batch batch = requireOwnedBatch(trainer, form.getBatchId());
        LiveClass lc = new LiveClass();
        applyClass(lc, batch, form);
        liveClassRepository.save(lc);
    }

    @Override
    @Transactional
    public void updateClass(User user, Long id, TrainerClassFormDto form) {
        Trainer trainer = requireTrainer(user);
        LiveClass lc = requireOwnedClass(trainer, id);
        Batch batch = requireOwnedBatch(trainer, form.getBatchId());
        applyClass(lc, batch, form);
        liveClassRepository.save(lc);
    }

    @Override
    @Transactional
    public void deleteClass(User user, Long id) {
        LiveClass lc = requireOwnedClass(requireTrainer(user), id);
        liveClassRepository.delete(lc);
    }

    private void applyClass(LiveClass lc, Batch batch, TrainerClassFormDto form) {
        lc.setTopic(requireTrimmed(form.getTopic(), "Topic is required."));
        lc.setBatch(batch);
        if (form.getStartTime() == null) {
            throw new BusinessException("Start date/time is required.");
        }
        if (form.getEndTime() != null && form.getEndTime().isBefore(form.getStartTime())) {
            throw new BusinessException("End time cannot be before the start time.");
        }
        lc.setStartTime(form.getStartTime());
        lc.setEndTime(form.getEndTime());
        ClassMode mode = form.getMode() == null ? ClassMode.ONLINE : form.getMode();
        lc.setMode(mode);
        if (mode == ClassMode.ONLINE) {
            lc.setMeetingLink(requireTrimmed(form.getMeetingLink(), "A meeting link is required for online classes."));
            lc.setClassroomDetails(trimToNull(form.getClassroomDetails()));
        } else {
            lc.setClassroomDetails(requireTrimmed(form.getClassroomDetails(), "Classroom details are required for offline classes."));
            lc.setMeetingLink(trimToNull(form.getMeetingLink()));
        }
        lc.setStatus(form.getStatus() == null ? ClassStatus.SCHEDULED : form.getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public TrainerClassRosterDto attendanceRoster(User user, Long classId) {
        LiveClass lc = requireOwnedClass(requireTrainer(user), classId);
        Map<Long, Boolean> marked = attendanceRepository.findByLiveClass_Id(classId).stream()
                .collect(Collectors.toMap(a -> a.getStudent().getId(), Attendance::isPresent, (x, y) -> x));
        List<TrainerAttendanceRowDto> roster = new ArrayList<>();
        for (Enrollment e : enrollmentRepository.findByBatch_IdOrderByCreatedAtDesc(lc.getBatch().getId())) {
            Student student = e.getStudent();
            User su = student.getUser();
            boolean present = marked.getOrDefault(student.getId(), false);
            roster.add(new TrainerAttendanceRowDto()
                    .setStudentId(student.getId())
                    .setStudentName(fullName(su))
                    .setStudentEmail(su.getEmail())
                    .setPresent(present));
        }
        long presentCount = roster.stream().filter(TrainerAttendanceRowDto::isPresent).count();
        return new TrainerClassRosterDto()
                .setClassId(lc.getId())
                .setTopic(lc.getTopic())
                .setBatchName(lc.getBatch().getBatchName())
                .setCourseTitle(lc.getBatch().getCourse().getTitle())
                .setStartLabel(lc.getStartTime() == null ? null : lc.getStartTime().format(DATE_TIME))
                .setStatusLabel(label(lc.getStatus()))
                .setPresentCount(presentCount)
                .setTotalCount(roster.size())
                .setRoster(roster);
    }

    @Override
    @Transactional
    public void markAttendance(User user, Long classId, List<Long> presentStudentIds, String status) {
        LiveClass lc = requireOwnedClass(requireTrainer(user), classId);
        Set<Long> present = presentStudentIds == null ? Collections.emptySet() : new HashSet<>(presentStudentIds);
        for (Enrollment e : enrollmentRepository.findByBatch_IdOrderByCreatedAtDesc(lc.getBatch().getId())) {
            Student student = e.getStudent();
            Attendance attendance = attendanceRepository
                    .findByLiveClass_IdAndStudent_Id(classId, student.getId())
                    .orElseGet(() -> Attendance.builder().liveClass(lc).student(student).build());
            attendance.setPresent(present.contains(student.getId()));
            attendanceRepository.save(attendance);
        }
        if (status != null && !status.isBlank()
                && lc.getStatus() != ClassStatus.CANCELLED) {
            lc.setStatus(parseClassStatus(status));
            liveClassRepository.save(lc);
        }
    }

    // =====================================================================
    // Announcements -------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementDto> listAnnouncements(User user) {
        Trainer trainer = requireTrainer(user);
        List<Long> batchIds = ids(ownBatches(trainer), Batch::getId);
        if (batchIds.isEmpty()) {
            return Collections.emptyList();
        }
        return announcementRepository.findByBatch_IdInOrderByCreatedAtDesc(batchIds).stream()
                .map(this::toAnnouncementDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createAnnouncement(User user, AnnouncementDto dto) {
        Trainer trainer = requireTrainer(user);
        if (dto.getBatchId() == null) {
            throw new BusinessException("Select one of your batches to announce to.");
        }
        Batch batch = requireOwnedBatch(trainer, dto.getBatchId());
        Announcement a = new Announcement();
        a.setTitle(requireTrimmed(dto.getTitle(), "Title is required."));
        a.setContent(requireTrimmed(dto.getContent(), "Content is required."));
        a.setBatch(batch);
        Announcement saved = announcementRepository.save(a);
        // The batch's students are notified as soon as the trainer posts.
        notificationService.announcementPublished(saved);
    }

    @Override
    @Transactional
    public void deleteAnnouncement(User user, Long id) {
        Trainer trainer = requireTrainer(user);
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found."));
        if (a.getBatch() == null || !trainer.getId().equals(a.getBatch().getTrainer().getId())) {
            throw new AccessDeniedException("You can only delete announcements for your own batches.");
        }
        announcementRepository.delete(a);
    }

    // =====================================================================
    // Ownership helpers
    // =====================================================================
    private Trainer requireTrainer(User user) {
        if (user == null) {
            throw new AccessDeniedException("You must be signed in.");
        }
        return trainerRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new BusinessException("Trainer profile not found for this account."));
    }

    private List<Course> ownCourses(Trainer trainer) {
        return courseRepository.findByTrainer_IdAndDeletedFalseOrderByTitleAsc(trainer.getId());
    }

    private List<Batch> ownBatches(Trainer trainer) {
        return batchRepository.findByTrainer_IdOrderByCreatedAtDesc(trainer.getId());
    }

    private List<Enrollment> trainerEnrollments(Trainer trainer) {
        List<Long> batchIds = ids(ownBatches(trainer), Batch::getId);
        return batchIds.isEmpty() ? Collections.emptyList()
                : enrollmentRepository.findByBatch_IdInOrderByCreatedAtDesc(batchIds);
    }

    private Course requireOwnedCourse(Trainer trainer, Long courseId) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found."));
        if (course.getTrainer() == null || !trainer.getId().equals(course.getTrainer().getId())) {
            throw new AccessDeniedException("This course is not assigned to you.");
        }
        return course;
    }

    private Batch requireOwnedBatch(Trainer trainer, Long batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found."));
        if (batch.getTrainer() == null || !trainer.getId().equals(batch.getTrainer().getId())) {
            throw new AccessDeniedException("This batch is not assigned to you.");
        }
        return batch;
    }

    private Assignment requireOwnedAssignment(Trainer trainer, Long id) {
        Assignment a = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found."));
        assertOwnedAssignmentEntity(trainer, a);
        return a;
    }

    private void assertOwnedAssignmentEntity(Trainer trainer, Assignment a) {
        if (a.getBatch() == null || a.getBatch().getTrainer() == null
                || !trainer.getId().equals(a.getBatch().getTrainer().getId())) {
            throw new AccessDeniedException("This assignment belongs to another trainer.");
        }
    }

    private LiveClass requireOwnedClass(Trainer trainer, Long id) {
        LiveClass lc = liveClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found."));
        if (lc.getBatch() == null || lc.getBatch().getTrainer() == null
                || !trainer.getId().equals(lc.getBatch().getTrainer().getId())) {
            throw new AccessDeniedException("This class belongs to another trainer.");
        }
        return lc;
    }

    private Quiz requireOwnedQuiz(Trainer trainer, Long id) {
        Quiz q = quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found."));
        if (q.getCourse() == null || q.getCourse().getTrainer() == null
                || !trainer.getId().equals(q.getCourse().getTrainer().getId())) {
            throw new AccessDeniedException("This quiz belongs to another trainer.");
        }
        return q;
    }

    // =====================================================================
    // Mapping helpers
    // =====================================================================
    private TrainerDto toTrainerDto(Trainer trainer) {
        User u = trainer.getUser();
        return new TrainerDto()
                .setId(trainer.getId())
                .setUserId(u.getId())
                .setFirstName(u.getFirstName())
                .setLastName(u.getLastName())
                .setFullName(fullName(u))
                .setEmail(u.getEmail())
                .setPhone(u.getPhone())
                .setExpertise(trainer.getExpertise())
                .setBio(trainer.getBio())
                .setActive(u.isActive())
                .setCourseCount(courseRepository.findByTrainer_IdAndDeletedFalseOrderByTitleAsc(trainer.getId()).size())
                .setBatchCount(batchRepository.findByTrainer_IdOrderByCreatedAtDesc(trainer.getId()).size())
                .setCreatedAt(trainer.getCreatedAt());
    }

    private BatchDto toBatchDto(Batch batch) {
        return new BatchDto()
                .setId(batch.getId())
                .setBatchName(batch.getBatchName())
                .setCourseId(batch.getCourse().getId())
                .setCourseTitle(batch.getCourse().getTitle())
                .setTrainerId(batch.getTrainer().getId())
                .setTrainerName(fullName(batch.getTrainer().getUser()))
                .setStartDate(batch.getStartDate())
                .setEndDate(batch.getEndDate())
                .setStartTime(batch.getStartTime())
                .setEndTime(batch.getEndTime())
                .setMode(batch.getMode())
                .setStatus(batch.getStatus())
                .setStatusLabel(label(batch.getStatus()))
                .setMaxSeats(batch.getMaxSeats())
                .setEnrolledCount(enrollmentRepository.countByBatch_Id(batch.getId()));
    }

    private LiveClassDto toLiveClassDto(LiveClass lc) {
        boolean upcoming = lc.getStartTime() != null && lc.getStartTime().isAfter(LocalDateTime.now())
                && lc.getStatus() != ClassStatus.CANCELLED;
        return new LiveClassDto()
                .setId(lc.getId())
                .setTopic(lc.getTopic())
                .setBatchId(lc.getBatch().getId())
                .setBatchName(lc.getBatch().getBatchName())
                .setCourseTitle(lc.getBatch().getCourse().getTitle())
                .setStartTime(lc.getStartTime())
                .setEndTime(lc.getEndTime())
                .setMode(lc.getMode() == null ? ClassMode.ONLINE : lc.getMode())
                .setModeLabel(lc.getMode() == null ? ClassMode.ONLINE.getLabel() : lc.getMode().getLabel())
                .setMeetingLink(lc.getMeetingLink())
                .setClassroomDetails(lc.getClassroomDetails())
                .setStatus(lc.getStatus())
                .setStatusLabel(label(lc.getStatus()))
                .setUpcoming(upcoming)
                .setStartLabel(lc.getStartTime() == null ? null : lc.getStartTime().format(DATE_TIME));
    }

    private AnnouncementDto toAnnouncementDto(Announcement a) {
        boolean global = a.getBatch() == null;
        return new AnnouncementDto()
                .setId(a.getId())
                .setTitle(a.getTitle())
                .setContent(a.getContent())
                .setBatchId(global ? null : a.getBatch().getId())
                .setBatchName(global ? "All students" : a.getBatch().getBatchName())
                .setGlobal(global)
                .setCreatedAt(a.getCreatedAt());
    }

    private TrainerSubmissionDto toSubmissionDto(Submission s, Assignment a) {
        User su = s.getStudent().getUser();
        return new TrainerSubmissionDto()
                .setSubmissionId(s.getId())
                .setAssignmentId(a.getId())
                .setStudentId(s.getStudent().getId())
                .setAssignmentTitle(a.getTitle())
                .setCourseTitle(a.getBatch().getCourse().getTitle())
                .setBatchName(a.getBatch().getBatchName())
                .setStudentName(fullName(su))
                .setStudentEmail(su.getEmail())
                .setFileUrl(s.getFileUrl())
                .setStatus(s.getStatus().name())
                .setStatusLabel(label(s.getStatus()))
                .setScore(s.getScore())
                .setFeedback(s.getFeedback())
                .setDueDate(a.getDueDate())
                .setMaxMarks(a.getMaxMarks())
                .setLate(s.isLate())
                .setSubmittedAt(s.getSubmittedAt() != null ? s.getSubmittedAt() : s.getCreatedAt());
    }

    private QuizQuestionFormDto toQuestionDto(QuizQuestion q) {
        return new QuizQuestionFormDto()
                .setId(q.getId())
                .setQuizId(q.getQuiz().getId())
                .setQuestionText(q.getQuestionText())
                .setOptionA(q.getOptionA())
                .setOptionB(q.getOptionB())
                .setOptionC(q.getOptionC())
                .setOptionD(q.getOptionD())
                .setCorrectOption(q.getCorrectOption());
    }

    private void applyQuestion(QuizQuestion q, QuizQuestionFormDto form) {
        q.setQuestionText(requireTrimmed(form.getQuestionText(), "Question text is required."));
        q.setOptionA(trimToNull(form.getOptionA()));
        q.setOptionB(trimToNull(form.getOptionB()));
        q.setOptionC(trimToNull(form.getOptionC()));
        q.setOptionD(trimToNull(form.getOptionD()));
        String correct = requireTrimmed(form.getCorrectOption(), "Select the correct option.").toUpperCase(Locale.ROOT);
        if (!List.of("A", "B", "C", "D").contains(correct)) {
            throw new BusinessException("The correct option must be A, B, C or D.");
        }
        String answer = switch (correct) {
            case "A" -> q.getOptionA();
            case "B" -> q.getOptionB();
            case "C" -> q.getOptionC();
            default -> q.getOptionD();
        };
        if (answer == null) {
            throw new BusinessException("The correct option cannot be blank — provide text for option " + correct + ".");
        }
        q.setCorrectOption(correct);
    }

    private List<Submission> submissionsForAssignments(List<Assignment> assignments) {
        if (assignments.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = assignments.stream().map(Assignment::getId).collect(Collectors.toList());
        return submissionRepository.findByAssignment_IdInOrderByCreatedAtDesc(ids);
    }

    private List<TrainerSubmissionDto> recentPendingSubmissions(List<Assignment> assignments) {
        Map<Long, Assignment> byId = assignments.stream().collect(Collectors.toMap(Assignment::getId, a -> a, (x, y) -> x));
        return submissionsForAssignments(assignments).stream()
                .filter(s -> s.getStatus() == SubmissionStatus.PENDING)
                .sorted(Comparator.comparing(Submission::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .map(s -> toSubmissionDto(s, byId.get(s.getAssignment().getId())))
                .collect(Collectors.toList());
    }

    private long[] attendanceStats(Long studentId) {
        List<Attendance> all = attendanceRepository.findByStudent_Id(studentId);
        long present = all.stream().filter(Attendance::isPresent).count();
        return new long[]{present, all.size()};
    }

    private SubmissionStatus parseSubmissionStatus(String status, Integer score) {
        if (status != null && !status.isBlank()) {
            try {
                return SubmissionStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                throw new BusinessException("Unknown submission status.");
            }
        }
        return score != null ? SubmissionStatus.GRADED : SubmissionStatus.PENDING;
    }

    private ClassStatus parseClassStatus(String status) {
        try {
            return ClassStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            throw new BusinessException("Unknown class status.");
        }
    }

    private int percent(long done, long total) {
        return total == 0 ? 0 : (int) Math.round(Math.min(done, total) * 100.0 / total);
    }

    private <T> List<Long> ids(List<T> items, java.util.function.Function<T, Long> getter) {
        return items.stream().map(getter).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private String fullName(User user) {
        if (user == null) {
            return "";
        }
        return ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
    }

    private String label(Enum<?> value) {
        if (value == null) {
            return "";
        }
        String[] parts = value.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private String requireTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
