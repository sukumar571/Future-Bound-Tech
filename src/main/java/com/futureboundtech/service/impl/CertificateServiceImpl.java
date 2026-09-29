package com.futureboundtech.service.impl;

import com.futureboundtech.dto.CertificateCriterionDto;
import com.futureboundtech.dto.CertificateDto;
import com.futureboundtech.dto.CertificateEligibilityDto;
import com.futureboundtech.dto.CertificateEvaluationDto;
import com.futureboundtech.dto.CertificateVerifyDto;
import com.futureboundtech.dto.NameValueDto;
import com.futureboundtech.entity.Assignment;
import com.futureboundtech.entity.Certificate;
import com.futureboundtech.entity.CertificateEligibility;
import com.futureboundtech.entity.Course;
import com.futureboundtech.entity.Enrollment;
import com.futureboundtech.entity.InstituteSettings;
import com.futureboundtech.entity.Quiz;
import com.futureboundtech.entity.QuizAttempt;
import com.futureboundtech.entity.Student;
import com.futureboundtech.entity.Submission;
import com.futureboundtech.entity.User;
import com.futureboundtech.enums.CertificateStatus;
import com.futureboundtech.enums.EnrollmentStatus;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.exception.ResourceNotFoundException;
import com.futureboundtech.repository.CertificateEligibilityRepository;
import com.futureboundtech.repository.CertificateRepository;
import com.futureboundtech.repository.AssignmentRepository;
import com.futureboundtech.repository.CourseRepository;
import com.futureboundtech.repository.EnrollmentRepository;
import com.futureboundtech.repository.InstituteSettingsRepository;
import com.futureboundtech.repository.LessonCompletionRepository;
import com.futureboundtech.repository.LessonRepository;
import com.futureboundtech.repository.QuizAttemptRepository;
import com.futureboundtech.repository.QuizRepository;
import com.futureboundtech.repository.StudentRepository;
import com.futureboundtech.repository.SubmissionRepository;
import com.futureboundtech.service.CertificateService;
import com.futureboundtech.service.CertificatePdfService;
import com.futureboundtech.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;
    private final CertificateEligibilityRepository eligibilityRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final LessonRepository lessonRepository;
    private final LessonCompletionRepository lessonCompletionRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final InstituteSettingsRepository instituteSettingsRepository;
    private final CertificatePdfService pdfService;
    private final NotificationService notificationService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // =====================================================================
    // Eligibility configuration
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<CertificateEligibilityDto> listConfigs() {
        return eligibilityRepository.findAllByOrderByCourse_IdAsc().stream()
                .map(this::toConfigDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateEligibilityDto getGlobalConfig() {
        return eligibilityRepository.findByCourse_IsNull()
                .map(this::toConfigDto)
                .orElseGet(() -> toConfigDto(defaultConfig(null)));
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateEligibilityDto getConfigForCourse(Long courseId) {
        return eligibilityRepository.findByCourse_Id(courseId)
                .map(this::toConfigDto)
                .orElseGet(() -> {
                    CertificateEligibilityDto base = getGlobalConfig();
                    base.setCourseId(courseId);
                    base.setId(null);
                    return base;
                });
    }

    @Override
    @Transactional
    public void saveConfig(CertificateEligibilityDto dto) {
        CertificateEligibility config = dto.getId() != null
                ? eligibilityRepository.findById(dto.getId())
                    .orElseGet(() -> newConfigForCourse(dto.getCourseId()))
                : newConfigForCourse(dto.getCourseId());

        if (dto.getCourseId() != null) {
            Course course = enrollmentCourse(dto.getCourseId());
            config.setCourse(course);
        } else {
            config.setCourse(null);
        }

        config.setMinCourseCompletionPercent(clampPercent(dto.getMinCourseCompletionPercent(), 100));
        config.setMinAssignmentCompletionPercent(clampPercent(dto.getMinAssignmentCompletionPercent(), 0));
        config.setMinQuizAveragePercent(clampPercent(dto.getMinQuizAveragePercent(), 0));
        config.setMinFinalAssessmentPercent(clampPercent(dto.getMinFinalAssessmentPercent(), 0));

        if (dto.getFinalAssessmentQuizId() != null && dto.getFinalAssessmentQuizId() > 0) {
            Quiz quiz = quizRepository.findById(dto.getFinalAssessmentQuizId())
                    .orElseThrow(() -> new BusinessException("Selected final assessment quiz no longer exists."));
            if (config.getCourse() != null && (quiz.getCourse() == null || !quiz.getCourse().getId().equals(config.getCourse().getId()))) {
                throw new BusinessException("The final assessment quiz must belong to the same course.");
            }
            config.setFinalAssessmentQuiz(quiz);
        } else {
            config.setFinalAssessmentQuiz(null);
        }

        config.setManualApprovalRequired(dto.isManualApprovalRequired());
        if (StringUtils.hasText(dto.getCertificateTitle())) {
            config.setCertificateTitle(dto.getCertificateTitle().trim());
        }
        config.setSignatoryName(trimToNull(dto.getSignatoryName()));
        config.setSignatoryDesignation(trimToNull(dto.getSignatoryDesignation()));
        eligibilityRepository.save(config);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NameValueDto> courseOptions() {
        return courseRepository.findByDeletedFalseOrderByTitleAsc().stream()
                .map(c -> new NameValueDto(c.getTitle(), c.getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NameValueDto> quizOptions() {
        return quizRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(Quiz::isPublished)
                .map(q -> new NameValueDto(q.getTitle() + " — "
                        + (q.getCourse() == null ? "unassigned" : q.getCourse().getTitle()), q.getId()))
                .collect(Collectors.toList());
    }

    private CertificateEligibility newConfigForCourse(Long courseId) {
        if (courseId == null) {
            return eligibilityRepository.findByCourse_IsNull()
                    .orElseGet(() -> CertificateEligibility.builder().build());
        }
        return eligibilityRepository.findByCourse_Id(courseId)
                .orElseGet(() -> CertificateEligibility.builder()
                        .course(enrollmentCourse(courseId))
                        .build());
    }

    /** Resolves the course a per-course eligibility row applies to. */
    private Course enrollmentCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException("Unknown course."));
    }

    // =====================================================================
    // Evaluation
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public CertificateEvaluationDto evaluateEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found."));
        return evaluate(enrollment, effectiveConfig(enrollment.getCourse().getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CertificateEvaluationDto> evaluationsForCourse(Long courseId) {
        CertificateEligibility config = effectiveConfig(courseId);
        return enrollmentRepository.findByCourse_Id(courseId).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE || e.getStatus() == EnrollmentStatus.COMPLETED)
                .map(e -> evaluate(e, config))
                .sorted(Comparator.comparing(CertificateEvaluationDto::isEligible).reversed()
                        .thenComparing(CertificateEvaluationDto::getStudentName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    private CertificateEvaluationDto evaluate(Enrollment enrollment, CertificateEligibility config) {
        Student student = enrollment.getStudent();
        Course course = enrollment.getCourse();
        User user = student.getUser();

        CertificateEvaluationDto dto = new CertificateEvaluationDto()
                .setEnrollmentId(enrollment.getId())
                .setStudentId(student.getId())
                .setStudentName(fullName(user))
                .setStudentEmail(user.getEmail())
                .setCourseId(course.getId())
                .setCourseTitle(course.getTitle())
                .setEnrollmentStatusLabel(pretty(enrollment.getStatus()));

        List<CertificateCriterionDto> criteria = new ArrayList<>();

        // 1. Enrollment must be active or completed.
        boolean seatPaid = enrollment.getStatus() == EnrollmentStatus.ACTIVE
                || enrollment.getStatus() == EnrollmentStatus.COMPLETED;
        criteria.add(new CertificateCriterionDto()
                .setLabel("Paid enrollment")
                .setRequirement("Active or completed")
                .setActual(pretty(enrollment.getStatus()))
                .setMet(seatPaid));

        // 2. Course completion percentage.
        int completionPercent = courseCompletionPercent(enrollment);
        criteria.add(new CertificateCriterionDto()
                .setLabel("Course completion")
                .setRequirement("≥ " + config.getMinCourseCompletionPercent() + "%")
                .setActual(completionPercent + "%")
                .setMet(completionPercent >= config.getMinCourseCompletionPercent()));

        // 3. Assignment completion.
        criteria.add(assignmentCriterion(student, enrollment, config));

        // 4. Quiz average score.
        criteria.add(quizCriterion(student, course, config));

        // 5. Final assessment.
        criteria.add(finalAssessmentCriterion(student, config));

        boolean eligible = criteria.stream().allMatch(CertificateCriterionDto::isMet);
        dto.setCriteria(criteria).setEligible(eligible);

        certificateRepository.findByEnrollment_Id(enrollment.getId()).ifPresent(c -> {
            dto.setHasCertificate(true)
                    .setCertificateId(c.getId())
                    .setCertificateNumber(c.getCertificateNumber())
                    .setCertificateStatus(c.getStatus())
                    .setCertificateStatusLabel(pretty(c.getStatus()));
        });
        return dto;
    }

    private int courseCompletionPercent(Enrollment enrollment) {
        if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
            return 100;
        }
        long total = lessonRepository.countByModule_Course_IdAndPublishedTrue(enrollment.getCourse().getId());
        long done = lessonCompletionRepository.countByStudent_IdAndLesson_Module_Course_Id(
                enrollment.getStudent().getId(), enrollment.getCourse().getId());
        if (total == 0) {
            return 0;
        }
        return (int) Math.round(Math.min(done, total) * 100.0 / total);
    }

    private CertificateCriterionDto assignmentCriterion(Student student, Enrollment enrollment,
                                                         CertificateEligibility config) {
        CertificateCriterionDto row = new CertificateCriterionDto()
                .setLabel("Assignment completion")
                .setRequirement("≥ " + config.getMinAssignmentCompletionPercent() + "% submitted");
        if (config.getMinAssignmentCompletionPercent() == null
                || config.getMinAssignmentCompletionPercent() <= 0) {
            return row.setActual("not required").setMet(true).setSkipped(true);
        }
        List<Assignment> assignments = assignmentRepository
                .findForCourses(List.of(enrollment.getCourse().getId())).stream()
                .filter(Assignment::isPublished)
                .filter(a -> enrollment.getBatch() == null
                        || (a.getBatch() != null && a.getBatch().getId().equals(enrollment.getBatch().getId())))
                .collect(Collectors.toList());
        if (assignments.isEmpty()) {
            return row.setActual("no assignments published").setMet(true).setSkipped(true);
        }
        Set<Long> assignmentIds = assignments.stream().map(Assignment::getId).collect(Collectors.toSet());
        long submitted = submissionRepository.findByStudent_IdOrderByCreatedAtDesc(student.getId()).stream()
                .filter(s -> s.getAssignment() != null && assignmentIds.contains(s.getAssignment().getId()))
                .filter(s -> s.getSubmittedAt() != null)
                .map(s -> s.getAssignment().getId())
                .distinct()
                .count();
        int percent = (int) Math.round(submitted * 100.0 / assignments.size());
        return row.setActual(submitted + " / " + assignments.size() + " (" + percent + "%)")
                .setMet(percent >= config.getMinAssignmentCompletionPercent());
    }

    private CertificateCriterionDto quizCriterion(Student student, Course course, CertificateEligibility config) {
        CertificateCriterionDto row = new CertificateCriterionDto()
                .setLabel("Quiz score (average of best attempts)")
                .setRequirement("≥ " + config.getMinQuizAveragePercent() + "%");
        if (config.getMinQuizAveragePercent() == null || config.getMinQuizAveragePercent() <= 0) {
            return row.setActual("not required").setMet(true).setSkipped(true);
        }
        List<Quiz> quizzes = quizRepository.findForCourses(List.of(course.getId())).stream()
                .filter(Quiz::isPublished)
                .collect(Collectors.toList());
        if (quizzes.isEmpty()) {
            return row.setActual("no published quizzes").setMet(true).setSkipped(true);
        }
        int sum = 0;
        int counted = 0;
        for (Quiz quiz : quizzes) {
            Integer best = bestQuizPercent(quiz.getId(), student.getId());
            if (best != null) {
                sum += best;
                counted++;
            }
        }
        if (counted == 0) {
            return row.setActual("no attempts").setMet(false);
        }
        int average = (int) Math.round(sum * 1.0 / counted);
        return row.setActual(average + "% over " + counted + " quiz(zes)")
                .setMet(average >= config.getMinQuizAveragePercent());
    }

    private CertificateCriterionDto finalAssessmentCriterion(Student student, CertificateEligibility config) {
        Quiz finalQuiz = config.getFinalAssessmentQuiz();
        CertificateCriterionDto row = new CertificateCriterionDto().setLabel("Final assessment");
        if (finalQuiz == null) {
            return row.setRequirement("not required").setActual("—").setMet(true).setSkipped(true);
        }
        row.setRequirement(finalQuiz.getTitle() + " ≥ " + config.getMinFinalAssessmentPercent() + "%");
        Integer best = bestQuizPercent(finalQuiz.getId(), student.getId());
        if (best == null) {
            return row.setActual("not attempted").setMet(false);
        }
        return row.setActual(best + "%").setMet(best >= config.getMinFinalAssessmentPercent());
    }

    /** Best percentage achieved on a quiz, or null when never attempted. */
    private Integer bestQuizPercent(Long quizId, Long studentId) {
        return quizAttemptRepository.findByQuiz_IdAndStudent_Id(quizId, studentId).stream()
                .map(QuizAttempt::getScoreObtained)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    // =====================================================================
    // Issuing / approving / revoking
    // =====================================================================
    @Override
    @Transactional
    public CertificateDto issue(Long enrollmentId, String adminEmail) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found."));
        Course course = enrollment.getCourse();
        if (!course.isCertificateEligible()) {
            throw new BusinessException("This course is not certificate-eligible.");
        }
        if (certificateRepository.findByEnrollment_Id(enrollmentId).isPresent()) {
            throw new BusinessException("A certificate record already exists for this enrollment.");
        }
        CertificateEligibility config = effectiveConfig(course.getId());
        CertificateEvaluationDto evaluation = evaluate(enrollment, config);
        if (!evaluation.isEligible()) {
            String unmet = evaluation.getCriteria().stream()
                    .filter(c -> !c.isMet())
                    .map(c -> c.getLabel() + " (" + c.getActual() + ", requires " + c.getRequirement() + ")")
                    .collect(Collectors.joining("; "));
            throw new BusinessException("Eligibility criteria not met: " + unmet);
        }
        Certificate certificate = createCertificate(enrollment, config, adminEmail);
        return toDto(certificate);
    }

    @Override
    @Transactional
    public int[] scanCourse(Long courseId, String adminEmail) {
        CertificateEligibility config = effectiveConfig(courseId);
        int issued = 0;
        int queued = 0;
        int skipped = 0;
        for (Enrollment enrollment : enrollmentRepository.findByCourse_Id(courseId)) {
            if (enrollment.getStatus() != EnrollmentStatus.ACTIVE
                    && enrollment.getStatus() != EnrollmentStatus.COMPLETED) {
                continue;
            }
            if (!enrollment.getCourse().isCertificateEligible()) {
                continue;
            }
            if (certificateRepository.findByEnrollment_Id(enrollment.getId()).isPresent()) {
                continue;
            }
            CertificateEvaluationDto evaluation = evaluate(enrollment, config);
            if (!evaluation.isEligible()) {
                skipped++;
                continue;
            }
            createCertificate(enrollment, config, adminEmail);
            if (config.isManualApprovalRequired()) {
                queued++;
            } else {
                issued++;
            }
        }
        return new int[]{issued, queued, skipped};
    }

    private Certificate createCertificate(Enrollment enrollment, CertificateEligibility config, String adminEmail) {
        User user = enrollment.getStudent().getUser();
        boolean pending = config.isManualApprovalRequired();
        Certificate certificate = Certificate.builder()
                .certificateNumber(nextCertificateNumber())
                .enrollment(enrollment)
                .status(pending ? CertificateStatus.PENDING_APPROVAL : CertificateStatus.ISSUED)
                .studentName(fullName(user))
                .courseTitle(enrollment.getCourse().getTitle())
                .issueDate(pending ? null : LocalDate.now())
                .issuedBy(adminEmail)
                .build();
        Certificate saved = certificateRepository.save(certificate);
        if (!pending) {
            // Auto-issued: the student hears about it now; otherwise approval sends the notice.
            notificationService.certificateIssued(saved);
        }
        return saved;
    }

    @Override
    @Transactional
    public void approve(Long certificateId, String adminEmail) {
        Certificate certificate = requireCertificate(certificateId);
        if (certificate.getStatus() != CertificateStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only certificates pending approval can be approved.");
        }
        certificate.setStatus(CertificateStatus.ISSUED);
        certificate.setIssueDate(LocalDate.now());
        certificate.setIssuedBy(adminEmail);
        certificateRepository.save(certificate);
        notificationService.certificateIssued(certificate);
    }

    @Override
    @Transactional
    public void reject(Long certificateId) {
        Certificate certificate = requireCertificate(certificateId);
        if (certificate.getStatus() != CertificateStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only certificates pending approval can be rejected.");
        }
        certificateRepository.delete(certificate);
    }

    @Override
    @Transactional
    public void revoke(Long certificateId, String reason, String adminEmail) {
        Certificate certificate = requireCertificate(certificateId);
        if (certificate.getStatus() != CertificateStatus.ISSUED) {
            throw new BusinessException("Only issued certificates can be revoked.");
        }
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException("A revocation reason is required.");
        }
        certificate.setStatus(CertificateStatus.REVOKED);
        certificate.setRevokeReason(reason.trim());
        certificate.setRevokedAt(LocalDateTime.now());
        certificateRepository.save(certificate);
    }

    private String nextCertificateNumber() {
        String prefix = "FBT-CERT-" + LocalDate.now().getYear() + "-";
        long seq = certificateRepository.countByCertificateNumberStartingWith(prefix) + 1;
        String candidate;
        do {
            candidate = prefix + String.format("%04d", seq);
            seq++;
        } while (certificateRepository.existsByCertificateNumber(candidate));
        return candidate;
    }

    // =====================================================================
    // Listing / search
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<CertificateDto> searchCertificates(String query, String statusFilter) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return certificateRepository.findAllByOrderByIssueDateDesc().stream()
                .filter(c -> !StringUtils.hasText(statusFilter) || c.getStatus().name().equalsIgnoreCase(statusFilter))
                .filter(c -> q.isEmpty()
                        || contains(c.getCertificateNumber(), q)
                        || contains(c.getStudentName(), q)
                        || contains(c.getCourseTitle(), q))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateDto getCertificate(Long id) {
        return toDto(requireCertificate(id));
    }

    // =====================================================================
    // PDF
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public byte[] renderCertificatePdf(Long certificateId, User requester, boolean admin) {
        Certificate certificate = requireCertificate(certificateId);
        if (certificate.getStatus() == CertificateStatus.REVOKED) {
            throw new BusinessException("This certificate has been revoked and can no longer be downloaded.");
        }
        if (certificate.getStatus() == CertificateStatus.PENDING_APPROVAL) {
            if (!admin) {
                throw new BusinessException("This certificate is awaiting approval.");
            }
        }
        if (!admin) {
            Student own = studentRepository.findByUser_Id(requester.getId())
                    .orElseThrow(() -> new BusinessException("No student profile found for this account."));
            if (!certificate.getEnrollment().getStudent().getId().equals(own.getId())) {
                throw new BusinessException("You can only download your own certificates.");
            }
        }
        CertificateEligibility config = effectiveConfig(certificate.getEnrollment().getCourse().getId());
        InstituteSettings settings = settingsOrNull();
        String institution = settings != null && StringUtils.hasText(settings.getName())
                ? settings.getName() : "Future Bound Tech";
        String logoUrl = settings == null ? null : resolveLogoUrl(settings.getLogoUrl());
        String verificationUrl = verificationUrl(certificate.getCertificateNumber());
        String signatory = settings == null ? null : settings.getCertificateSignatory();
        String signatoryTitle = settings == null ? null : settings.getCertificateSignatoryTitle();
        String footerNote = settings == null ? null : settings.getCertificateFooterNote();
        return pdfService.render(certificate, institution, logoUrl, config, verificationUrl,
                signatory, signatoryTitle, footerNote);
    }

    @Override
    public String pdfFileName(CertificateDto dto) {
        return "FBT-Certificate-" + dto.getCertificateNumber() + ".pdf";
    }

    // =====================================================================
    // Public verification
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public CertificateVerifyDto verify(String certificateNumber) {
        CertificateVerifyDto dto = new CertificateVerifyDto().setQueriedNumber(certificateNumber);
        Optional<Certificate> found = StringUtils.hasText(certificateNumber)
                ? certificateRepository.findByCertificateNumberIgnoreCase(certificateNumber.trim())
                : Optional.empty();
        if (found.isEmpty()) {
            return dto.setOutcome("NOT_FOUND").setValid(false);
        }
        Certificate certificate = found.get();
        dto.setStudentName(certificate.getStudentName())
                .setCourseTitle(certificate.getCourseTitle())
                .setIssueDate(certificate.getIssueDate())
                .setInstitutionName(instituteName());
        switch (certificate.getStatus()) {
            case ISSUED -> dto.setValid(true).setOutcome("VALID");
            case REVOKED -> dto.setValid(false).setOutcome("REVOKED")
                    .setRevokeReason(certificate.getRevokeReason());
            default -> dto.setValid(false).setOutcome("PENDING_APPROVAL");
        }
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public String instituteName() {
        InstituteSettings settings = settingsOrNull();
        return settings != null && StringUtils.hasText(settings.getName())
                ? settings.getName() : "Future Bound Tech";
    }

    // =====================================================================
    // Helpers
    // =====================================================================
    private CertificateEligibility effectiveConfig(Long courseId) {
        return eligibilityRepository.findByCourse_Id(courseId)
                .or(() -> eligibilityRepository.findByCourse_IsNull())
                .orElseGet(() -> defaultConfig(null));
    }

    private CertificateEligibility defaultConfig(Course course) {
        return CertificateEligibility.builder().course(course).build();
    }

    private InstituteSettings settingsOrNull() {
        return instituteSettingsRepository.findAll().stream().findFirst().orElse(null);
    }

    /** Turns a stored branding path into the /uploads URL the PDF loader expects. */
    private String resolveLogoUrl(String stored) {
        if (!StringUtils.hasText(stored)) {
            return null;
        }
        String value = stored.trim();
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("/")) {
            return value;
        }
        return "/uploads/" + value.replace("\\", "/");
    }

    private Certificate requireCertificate(Long id) {
        return certificateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found."));
    }

    private String verificationUrl(String number) {
        String base = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        return base + "/certificate/verify/" + number;
    }

    private CertificateDto toDto(Certificate certificate) {
        Enrollment enrollment = certificate.getEnrollment();
        return new CertificateDto()
                .setId(certificate.getId())
                .setCertificateNumber(certificate.getCertificateNumber())
                .setEnrollmentId(enrollment.getId())
                .setStudentName(certificate.getStudentName() != null
                        ? certificate.getStudentName() : fullName(enrollment.getStudent().getUser()))
                .setCourseTitle(certificate.getCourseTitle() != null
                        ? certificate.getCourseTitle() : enrollment.getCourse().getTitle())
                .setIssueDate(certificate.getIssueDate())
                .setCertificateUrl(certificate.getCertificateUrl())
                .setStatus(certificate.getStatus())
                .setStatusLabel(pretty(certificate.getStatus()))
                .setRevokeReason(certificate.getRevokeReason())
                .setIssuedBy(certificate.getIssuedBy())
                .setVerificationUrl(verificationUrl(certificate.getCertificateNumber()));
    }

    private CertificateEligibilityDto toConfigDto(CertificateEligibility config) {
        return new CertificateEligibilityDto()
                .setId(config.getId())
                .setCourseId(config.getCourse() == null ? null : config.getCourse().getId())
                .setCourseTitle(config.getCourse() == null ? null : config.getCourse().getTitle())
                .setMinCourseCompletionPercent(config.getMinCourseCompletionPercent())
                .setMinAssignmentCompletionPercent(config.getMinAssignmentCompletionPercent())
                .setMinQuizAveragePercent(config.getMinQuizAveragePercent())
                .setFinalAssessmentQuizId(config.getFinalAssessmentQuiz() == null
                        ? null : config.getFinalAssessmentQuiz().getId())
                .setMinFinalAssessmentPercent(config.getMinFinalAssessmentPercent())
                .setManualApprovalRequired(config.isManualApprovalRequired())
                .setCertificateTitle(config.getCertificateTitle())
                .setSignatoryName(config.getSignatoryName())
                .setSignatoryDesignation(config.getSignatoryDesignation());
    }

    private static int clampPercent(Integer value, int fallback) {
        if (value == null) {
            return fallback;
        }
        return Math.max(0, Math.min(100, value));
    }

    private static boolean contains(String haystack, String needleLower) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needleLower);
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String fullName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }

    private static String pretty(Enum<?> value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String part : value.name().toLowerCase(Locale.ROOT).split("_")) {
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
}
