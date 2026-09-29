package com.futureboundtech.service.impl;

import com.futureboundtech.dto.*;
import com.futureboundtech.entity.*;
import com.futureboundtech.enums.*;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.exception.ResourceNotFoundException;
import com.futureboundtech.repository.*;
import com.futureboundtech.service.AdminService;
import com.futureboundtech.service.FileStorageService;
import com.futureboundtech.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a");
    private static final Locale INR_LOCALE = new Locale("en", "IN");

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TrainerRepository trainerRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;
    private final BatchRepository batchRepository;
    private final LiveClassRepository liveClassRepository;
    private final CouponRepository couponRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final CertificateRepository certificateRepository;
    private final ReviewRepository reviewRepository;
    private final AnnouncementRepository announcementRepository;
    private final FaqItemRepository faqItemRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final AssignmentRepository assignmentRepository;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final SubmissionRepository submissionRepository;
    private final LessonRepository lessonRepository;
    private final LessonCompletionRepository lessonCompletionRepository;
    private final InstituteSettingsRepository instituteSettingsRepository;
    private final PasswordEncoder passwordEncoder;

    // =====================================================================
    // Dashboard analytics
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public AdminStatsDto dashboardStats() {
        LocalDateTime now = LocalDateTime.now();
        AdminStatsDto stats = new AdminStatsDto();

        List<Student> students = studentRepository.findAll();
        stats.setTotalStudents(students.size());
        stats.setActiveStudents(students.stream().filter(s -> s.getUser().isActive()).count());
        stats.setTotalTrainers(trainerRepository.count());
        stats.setTotalCourses(courseRepository.countByDeletedFalse());
        stats.setPublishedCourses(courseRepository.countByDeletedFalseAndStatus(CourseStatus.PUBLISHED));
        stats.setTotalEnrollments(enrollmentRepository.count());
        stats.setActiveEnrollments(enrollmentRepository.countByStatus(EnrollmentStatus.ACTIVE));
        stats.setTotalBatches(batchRepository.count());
        stats.setUpcomingClasses(liveClassRepository
                .findByStartTimeGreaterThanEqualAndStatusNotOrderByStartTimeAsc(now, ClassStatus.CANCELLED).size());
        stats.setCertificatesIssued(certificateRepository.count());
        stats.setUnreadMessages(contactMessageRepository.countByIsRepliedFalse());

        stats.setSuccessfulPayments(paymentRepository.countByStatus(PaymentStatus.SUCCESS));
        stats.setPendingPayments(paymentRepository.countByStatus(PaymentStatus.PENDING)
                + paymentRepository.countByStatus(PaymentStatus.CREATED));
        stats.setFailedPayments(paymentRepository.countByStatus(PaymentStatus.FAILED));
        stats.setTotalRevenue(formatInr(paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS)));

        stats.setRevenueByStatus(List.of(
                new NameValueDto("Successful", paymentRepository.countByStatus(PaymentStatus.SUCCESS)),
                new NameValueDto("Pending", paymentRepository.countByStatus(PaymentStatus.PENDING)
                        + paymentRepository.countByStatus(PaymentStatus.CREATED)),
                new NameValueDto("Failed", paymentRepository.countByStatus(PaymentStatus.FAILED)),
                new NameValueDto("Refunded", paymentRepository.countByStatus(PaymentStatus.REFUNDED))));

        stats.setRecentRegistrations(userRepository.findByRoleOrderByCreatedAtDesc(Role.STUDENT).stream()
                .limit(6).map(this::toUserBrief).collect(Collectors.toList()));

        Map<String, Long> byCourse = new LinkedHashMap<>();
        for (Course course : courseRepository.findByDeletedFalseOrderByTitleAsc()) {
            long count = enrollmentRepository.countByCourse_Id(course.getId());
            if (count > 0) {
                byCourse.put(course.getTitle(), count);
            }
        }
        stats.setEnrollmentsByCourse(byCourse.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(6)
                .map(e -> new NameValueDto(e.getKey(), e.getValue()))
                .collect(Collectors.toList()));

        Map<CourseCategory, Long> byCategory = courseRepository.findByDeletedFalseOrderByTitleAsc().stream()
                .collect(Collectors.groupingBy(Course::getCategory, Collectors.counting()));
        stats.setCoursesByCategory(Arrays.stream(CourseCategory.values())
                .map(c -> new NameValueDto(c.getLabel(), byCategory.getOrDefault(c, 0L)))
                .collect(Collectors.toList()));

        return stats;
    }

    // =====================================================================
    // Students
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<StudentDto> listStudents(String query, Boolean active) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return studentRepository.findAll().stream()
                .filter(s -> active == null || s.getUser().isActive() == active)
                .filter(s -> q.isEmpty() || matches(s.getUser(), q))
                .map(this::toStudentDto)
                .sorted(Comparator.comparing(StudentDto::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StudentDto getStudent(Long studentId) {
        return toStudentDto(requireStudent(studentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentDto> studentEnrollments(Long studentId) {
        return enrollmentRepository.findByStudent_IdOrderByCreatedAtDesc(studentId).stream()
                .map(this::toEnrollmentDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentDto> studentPayments(Long studentId) {
        return paymentRepository.findByStudent_IdOrderByCreatedAtDesc(studentId).stream()
                .map(this::toPaymentDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createStudent(StudentFormDto form) {
        requireUniqueEmail(form.getEmail(), null);
        requirePassword(form.getPassword());
        if (!Objects.equals(form.getPassword(), form.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match.");
        }
        User user = new User();
        applyUser(user, form.getFirstName(), form.getLastName(), form.getEmail(), form.getPhone(), Role.STUDENT);
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setActive(form.isActive());

        Student student = new Student();
        student.setUser(user);
        student.setEducation(trimToNull(form.getEducation()));
        user.setStudentProfile(student);

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateStudent(Long studentId, StudentFormDto form) {
        Student student = requireStudent(studentId);
        User user = student.getUser();
        requireUniqueEmail(form.getEmail(), user.getId());
        applyUser(user, form.getFirstName(), form.getLastName(), form.getEmail(), form.getPhone(), Role.STUDENT);
        user.setActive(form.isActive());
        student.setEducation(trimToNull(form.getEducation()));
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            if (!Objects.equals(form.getPassword(), form.getConfirmPassword())) {
                throw new BusinessException("Passwords do not match.");
            }
            user.setPassword(passwordEncoder.encode(form.getPassword()));
        }
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void setStudentActive(Long studentId, boolean active) {
        Student student = requireStudent(studentId);
        student.getUser().setActive(active);
        userRepository.save(student.getUser());
    }

    // =====================================================================
    // Trainers
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<TrainerDto> listTrainers(String query, Boolean active) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return trainerRepository.findAllWithUser().stream()
                .filter(t -> active == null || t.getUser().isActive() == active)
                .filter(t -> q.isEmpty() || matches(t.getUser(), q))
                .map(this::toTrainerDto)
                .sorted(Comparator.comparing(TrainerDto::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TrainerDto getTrainer(Long trainerId) {
        return toTrainerDto(requireTrainer(trainerId));
    }

    @Override
    @Transactional
    public void createTrainer(TrainerFormDto form) {
        requireUniqueEmail(form.getEmail(), null);
        requirePassword(form.getPassword());
        if (!Objects.equals(form.getPassword(), form.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match.");
        }
        User user = new User();
        applyUser(user, form.getFirstName(), form.getLastName(), form.getEmail(), form.getPhone(), Role.TRAINER);
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setActive(form.isActive());

        Trainer trainer = new Trainer();
        trainer.setUser(user);
        trainer.setExpertise(trimToNull(form.getExpertise()));
        trainer.setBio(trimToNull(form.getBio()));
        trainer.setExperienceYears(form.getExperienceYears());
        if (form.getPhotoFile() != null && !form.getPhotoFile().isEmpty()) {
            trainer.setPhotoUrl(fileStorageService.storeTrainerPhoto(form.getPhotoFile()));
        }
        user.setTrainerProfile(trainer);

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateTrainer(Long trainerId, TrainerFormDto form) {
        Trainer trainer = requireTrainer(trainerId);
        User user = trainer.getUser();
        requireUniqueEmail(form.getEmail(), user.getId());
        applyUser(user, form.getFirstName(), form.getLastName(), form.getEmail(), form.getPhone(), Role.TRAINER);
        user.setActive(form.isActive());
        trainer.setExpertise(trimToNull(form.getExpertise()));
        trainer.setBio(trimToNull(form.getBio()));
        trainer.setExperienceYears(form.getExperienceYears());
        if (form.getPhotoFile() != null && !form.getPhotoFile().isEmpty()) {
            String previous = trainer.getPhotoUrl();
            trainer.setPhotoUrl(fileStorageService.storeTrainerPhoto(form.getPhotoFile()));
            deleteStoredPhoto(previous);
        }
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            if (!Objects.equals(form.getPassword(), form.getConfirmPassword())) {
                throw new BusinessException("Passwords do not match.");
            }
            user.setPassword(passwordEncoder.encode(form.getPassword()));
        }
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void setTrainerActive(Long trainerId, boolean active) {
        Trainer trainer = requireTrainer(trainerId);
        trainer.getUser().setActive(active);
        userRepository.save(trainer.getUser());
    }

    /** Removes a previously uploaded trainer photo, ignoring external URLs and blanks. */
    private void deleteStoredPhoto(String stored) {
        if (stored == null || stored.isBlank()) {
            return;
        }
        String value = stored.trim();
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("/")) {
            return;
        }
        if (value.startsWith("trainers/")) {
            fileStorageService.deleteIfExists(value);
        }
    }

    /** Turns a stored relative photo path into a browser URL, leaving absolute URLs untouched. */
    private String toPhotoUrl(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        String value = stored.trim();
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("/")) {
            return value;
        }
        return "/uploads/" + value.replace("\\", "/");
    }

    // =====================================================================
    // Batches
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<BatchDto> listBatches() {
        return batchRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toBatchDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BatchDto getBatch(Long batchId) {
        return toBatchDto(requireBatch(batchId));
    }

    @Override
    @Transactional
    public void createBatch(BatchDto dto) {
        Batch batch = new Batch();
        applyBatch(batch, dto);
        batchRepository.save(batch);
    }

    @Override
    @Transactional
    public void updateBatch(Long batchId, BatchDto dto) {
        Batch batch = requireBatch(batchId);
        applyBatch(batch, dto);
        batchRepository.save(batch);
    }

    @Override
    @Transactional
    public void deleteBatch(Long batchId) {
        Batch batch = requireBatch(batchId);
        long enrolled = enrollmentRepository.countByBatch_Id(batchId);
        if (enrolled > 0) {
            throw new BusinessException("This batch has " + enrolled
                    + " enrolled students. Reassign them before deleting the batch.");
        }
        batchRepository.delete(batch);
    }

    @Override
    @Transactional
    public void setBatchPublished(Long batchId, boolean published) {
        Batch batch = requireBatch(batchId);
        batch.setPublished(published);
        batchRepository.save(batch);
    }

    @Override
    @Transactional
    public void setBatchEnrollmentStatus(Long batchId, BatchEnrollmentStatus status) {
        Batch batch = requireBatch(batchId);
        batch.setEnrollmentStatus(status == null ? BatchEnrollmentStatus.OPEN : status);
        batchRepository.save(batch);
    }

    // =====================================================================
    // Live classes
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<LiveClassDto> listLiveClasses() {
        return liveClassRepository.findAllByOrderByStartTimeDesc().stream()
                .map(this::toLiveClassDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LiveClassDto getLiveClass(Long id) {
        return toLiveClassDto(requireLiveClass(id));
    }

    @Override
    @Transactional
    public void createLiveClass(LiveClassDto dto) {
        LiveClass liveClass = new LiveClass();
        applyLiveClass(liveClass, dto);
        liveClassRepository.save(liveClass);
    }

    @Override
    @Transactional
    public void updateLiveClass(Long id, LiveClassDto dto) {
        LiveClass liveClass = requireLiveClass(id);
        applyLiveClass(liveClass, dto);
        liveClassRepository.save(liveClass);
    }

    @Override
    @Transactional
    public void deleteLiveClass(Long id) {
        liveClassRepository.delete(requireLiveClass(id));
    }

    // =====================================================================
    // Enrollments
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentDto> listEnrollments(String query, EnrollmentStatus status) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return enrollmentRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(e -> status == null || e.getStatus() == status)
                .filter(e -> q.isEmpty() || matches(e.getStudent().getUser(), q)
                        || e.getCourse().getTitle().toLowerCase(Locale.ROOT).contains(q))
                .map(this::toEnrollmentDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void setEnrollmentStatus(Long enrollmentId, EnrollmentStatus status) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found."));
        enrollment.setStatus(status);
        enrollmentRepository.save(enrollment);
    }

    // =====================================================================
    // Coupons
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<CouponDto> listCoupons() {
        return couponRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toCouponDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CouponDto getCoupon(Long id) {
        return toCouponDto(requireCoupon(id));
    }

    @Override
    @Transactional
    public void createCoupon(CouponDto dto) {
        String code = dto.getCode().trim();
        if (couponRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new BusinessException("A coupon with that code already exists.");
        }
        Coupon coupon = new Coupon();
        applyCouponDto(coupon, dto);
        coupon.setCode(code.toUpperCase(Locale.ROOT));
        couponRepository.save(coupon);
    }

    @Override
    @Transactional
    public void updateCoupon(Long id, CouponDto dto) {
        Coupon coupon = requireCoupon(id);
        String code = dto.getCode().trim();
        couponRepository.findByCodeIgnoreCase(code).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BusinessException("Another coupon already uses that code.");
            }
        });
        applyCouponDto(coupon, dto);
        coupon.setCode(code.toUpperCase(Locale.ROOT));
        couponRepository.save(coupon);
    }

    /** Copies the editable coupon fields from the admin form onto the entity. */
    private void applyCouponDto(Coupon coupon, CouponDto dto) {
        coupon.setDiscountType(dto.getDiscountType() == null
                ? com.futureboundtech.enums.CouponDiscountType.FIXED : dto.getDiscountType());
        coupon.setDiscountAmount(dto.getDiscountAmount() == null ? BigDecimal.ZERO : dto.getDiscountAmount());
        coupon.setDiscountPercentage(dto.getDiscountPercentage());
        coupon.setMaxDiscount(dto.getMaxDiscount());
        coupon.setValidFrom(dto.getValidFrom());
        coupon.setValidUntil(dto.getValidUntil());
        coupon.setUsageLimit(dto.getUsageLimit());
        coupon.setActive(dto.isActive());
        Set<Long> courseIds = new HashSet<>();
        if (dto.getApplicableCourseIds() != null) {
            dto.getApplicableCourseIds().stream().filter(Objects::nonNull).forEach(courseIds::add);
        }
        coupon.setApplicableCourseIds(courseIds);
    }

    @Override
    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = requireCoupon(id);
        long used = paymentRepository.countByCoupon_Id(id);
        if (used > 0) {
            throw new BusinessException("This coupon has been used on " + used
                    + " payment(s) and cannot be deleted. Deactivate it instead.");
        }
        couponRepository.delete(coupon);
    }

    // =====================================================================
    // Announcements
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementDto> listAnnouncements() {
        return announcementRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toAnnouncementDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AnnouncementDto getAnnouncement(Long id) {
        return toAnnouncementDto(requireAnnouncement(id));
    }

    @Override
    @Transactional
    public void createAnnouncement(AnnouncementDto dto) {
        Announcement announcement = new Announcement();
        applyAnnouncement(announcement, dto);
        Announcement saved = announcementRepository.save(announcement);
        // Publishing is the trigger: every student in the audience gets an inbox copy.
        notificationService.announcementPublished(saved);
    }

    @Override
    @Transactional
    public void updateAnnouncement(Long id, AnnouncementDto dto) {
        Announcement announcement = requireAnnouncement(id);
        applyAnnouncement(announcement, dto);
        announcementRepository.save(announcement);
    }

    @Override
    @Transactional
    public void deleteAnnouncement(Long id) {
        announcementRepository.delete(requireAnnouncement(id));
    }

    // =====================================================================
    // Contact messages
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<ContactMessageDto> listContactMessages() {
        return contactMessageRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toContactDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactMessageDto> listContactMessages(String query, ContactStatus status) {
        String q = trimToNull(query);
        return contactMessageRepository.search(q, status).stream()
                .map(this::toContactDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void setContactReplied(Long id, boolean replied) {
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found."));
        message.setReplied(replied);
        contactMessageRepository.save(message);
    }

    @Override
    @Transactional
    public void setContactStatus(Long id, ContactStatus status) {
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found."));
        ContactStatus resolved = status == null ? ContactStatus.NEW : status;
        message.setStatus(resolved);
        message.setReplied(resolved == ContactStatus.RESOLVED);
        contactMessageRepository.save(message);
    }

    // =====================================================================
    // Certificates
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<CertificateDto> listCertificates() {
        return certificateRepository.findAllByOrderByIssueDateDesc().stream()
                .filter(c -> c.getStatus() == com.futureboundtech.enums.CertificateStatus.ISSUED)
                .map(this::toCertificateDto)
                .collect(Collectors.toList());
    }

    // =====================================================================
    // Reviews / testimonials
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<ReviewDto> listReviews() {
        return reviewRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toReviewDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewDto getReview(Long id) {
        return toReviewDto(requireReview(id));
    }

    @Override
    @Transactional
    public void setReviewApproved(Long id, boolean approved) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found."));
        review.setApproved(approved);
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void createReview(ReviewDto dto) {
        Review review = new Review();
        applyReview(review, dto);
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void updateReview(Long id, ReviewDto dto) {
        Review review = requireReview(id);
        applyReview(review, dto);
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void deleteReview(Long id) {
        reviewRepository.delete(requireReview(id));
    }

    @Override
    @Transactional
    public void setReviewPublished(Long id, boolean published) {
        Review review = requireReview(id);
        review.setPublished(published);
        reviewRepository.save(review);
    }

    private void applyReview(Review review, ReviewDto dto) {
        review.setAuthorName(trimToNull(dto.getAuthorName()));
        review.setContextLabel(trimToNull(dto.getContextLabel()));
        review.setRating(dto.getRating() == null ? 5 : dto.getRating());
        review.setComment(trimToNull(dto.getComment()));
        review.setApproved(dto.isApproved());
        review.setDemo(dto.isDemo());
        review.setPublished(dto.isPublished());
    }

    private Review requireReview(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found."));
    }

    // =====================================================================
    // FAQs
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<FaqDto> listFaqs() {
        return faqItemRepository.findAllByOrderBySortOrderAscCreatedAtAsc().stream()
                .map(this::toFaqDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FaqDto getFaq(Long id) {
        return toFaqDto(requireFaq(id));
    }

    @Override
    @Transactional
    public void createFaq(FaqDto dto) {
        FaqItem item = new FaqItem();
        applyFaq(item, dto);
        faqItemRepository.save(item);
    }

    @Override
    @Transactional
    public void updateFaq(Long id, FaqDto dto) {
        FaqItem item = requireFaq(id);
        applyFaq(item, dto);
        faqItemRepository.save(item);
    }

    @Override
    @Transactional
    public void deleteFaq(Long id) {
        faqItemRepository.delete(requireFaq(id));
    }

    @Override
    @Transactional
    public void setFaqPublished(Long id, boolean published) {
        FaqItem item = requireFaq(id);
        item.setPublished(published);
        faqItemRepository.save(item);
    }

    private void applyFaq(FaqItem item, FaqDto dto) {
        item.setQuestion(dto.getQuestion().trim());
        item.setAnswer(dto.getAnswer().trim());
        item.setCategory(trimToNull(dto.getCategory()));
        item.setPublished(dto.isPublished());
        item.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
    }

    private FaqItem requireFaq(Long id) {
        return faqItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ not found."));
    }

    private FaqDto toFaqDto(FaqItem item) {
        return new FaqDto()
                .setId(item.getId())
                .setQuestion(item.getQuestion())
                .setAnswer(item.getAnswer())
                .setCategory(item.getCategory())
                .setPublished(item.isPublished())
                .setSortOrder(item.getSortOrder())
                .setCreatedAt(item.getCreatedAt());
    }

    // =====================================================================
    // Assignments & quizzes
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<AssignmentDto> listAssignments() {
        return assignmentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(a -> new AssignmentDto()
                        .setId(a.getId())
                        .setTitle(a.getTitle())
                        .setCourseTitle(a.getBatch().getCourse().getTitle())
                        .setBatchName(a.getBatch().getBatchName())
                        .setDueDate(a.getDueDate())
                        .setDueLabel(a.getDueDate() == null ? null : a.getDueDate().format(DATE_TIME))
                        .setSubmissionCount(submissionRepository.countByAssignment_Id(a.getId()))
                        .setCreatedAt(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizDto> listQuizzes() {
        return quizRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(q -> new QuizDto()
                        .setId(q.getId())
                        .setTitle(q.getTitle())
                        .setCourseTitle(q.getCourse() == null ? "General" : q.getCourse().getTitle())
                        .setQuestionCount(quizQuestionRepository.countByQuiz_Id(q.getId()))
                        .setTimeLimitMinutes(q.getTimeLimitMinutes())
                        .setPassingScore(q.getPassingScore())
                        .setAttemptCount(quizAttemptRepository.countByQuiz_Id(q.getId()))
                        .setCreatedAt(q.getCreatedAt()))
                .collect(Collectors.toList());
    }

    // =====================================================================
    // Settings
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public InstituteSettingsDto getSettings() {
        InstituteSettings settings = instituteSettingsRepository.findAll().stream().findFirst().orElse(null);
        InstituteSettingsDto dto = new InstituteSettingsDto();
        if (settings == null) {
            dto.setName("Future Bound Tech");
            dto.setTagLine("Learn Today. Build Tomorrow.");
            dto.setPhone1("8978866005");
            dto.setPhone2("7893702635");
            dto.setTrainingModes("Online + Offline");
            dto.setDefaultDurationMonths(3);
            dto.setDefaultLevel(com.futureboundtech.enums.CourseLevel.BEGINNER);
            dto.setDefaultTrainingMode(com.futureboundtech.enums.TrainingMode.HYBRID);
            dto.setDefaultCertificateEligible(true);
            dto.setCurrency("INR");
            dto.setPaymentGatewayLabel("Razorpay");
            dto.setAllowCoupons(true);
            dto.setEmailNotifications(true);
            return dto;
        }
        dto.setId(settings.getId())
                .setName(settings.getName())
                .setTagLine(settings.getTagLine())
                .setPhone1(settings.getPhone1())
                .setPhone2(settings.getPhone2())
                .setEmail(settings.getEmail())
                .setWebsite(settings.getWebsite())
                .setAddress(settings.getAddress())
                .setTrainingModes(settings.getTrainingModes())
                .setLogoUrl(toPhotoUrl(settings.getLogoUrl()))
                .setFaviconUrl(toPhotoUrl(settings.getFaviconUrl()))
                .setPrimaryColor(settings.getPrimaryColor())
                .setSecondaryColor(settings.getSecondaryColor())
                .setAccentColor(settings.getAccentColor())
                .setFacebookUrl(settings.getFacebookUrl())
                .setTwitterUrl(settings.getTwitterUrl())
                .setInstagramUrl(settings.getInstagramUrl())
                .setLinkedinUrl(settings.getLinkedinUrl())
                .setYoutubeUrl(settings.getYoutubeUrl())
                .setDefaultRegistrationFee(settings.getDefaultRegistrationFee())
                .setCurrency(settings.getCurrency())
                .setDefaultDurationMonths(settings.getDefaultDurationMonths())
                .setDefaultLevel(settings.getDefaultLevel())
                .setDefaultTrainingMode(settings.getDefaultTrainingMode())
                .setDefaultCertificateEligible(settings.isDefaultCertificateEligible())
                .setDefaultPublicListed(settings.isDefaultPublicListed())
                .setPaymentGatewayLabel(settings.getPaymentGatewayLabel())
                .setPaymentInstructions(settings.getPaymentInstructions())
                .setAllowCoupons(settings.isAllowCoupons())
                .setEmailNotifications(settings.isEmailNotifications())
                .setWhatsappNotifications(settings.isWhatsappNotifications())
                .setSmsNotifications(settings.isSmsNotifications())
                .setCertificateSignatory(settings.getCertificateSignatory())
                .setCertificateSignatoryTitle(settings.getCertificateSignatoryTitle())
                .setCertificateFooterNote(settings.getCertificateFooterNote())
                .setPrivacyPolicy(settings.getPrivacyPolicy())
                .setTermsAndConditions(settings.getTermsAndConditions());
        return dto;
    }

    @Override
    @Transactional
    public void saveSettings(InstituteSettingsDto dto) {
        InstituteSettings settings = instituteSettingsRepository.findAll().stream().findFirst()
                .orElseGet(() -> InstituteSettings.builder().name(dto.getName()).build());
        settings.setName(dto.getName().trim());
        settings.setTagLine(trimToNull(dto.getTagLine()));
        settings.setPhone1(trimToNull(dto.getPhone1()));
        settings.setPhone2(trimToNull(dto.getPhone2()));
        settings.setEmail(trimToNull(dto.getEmail()));
        settings.setWebsite(trimToNull(dto.getWebsite()));
        settings.setAddress(trimToNull(dto.getAddress()));
        settings.setTrainingModes(trimToNull(dto.getTrainingModes()));
        settings.setPrimaryColor(normaliseHex(dto.getPrimaryColor()));
        settings.setSecondaryColor(normaliseHex(dto.getSecondaryColor()));
        settings.setAccentColor(normaliseHex(dto.getAccentColor()));

        // Logo: replace only when a new file is uploaded; a typed URL is stored as-is.
        if (dto.getLogoFile() != null && !dto.getLogoFile().isEmpty()) {
            String previous = settings.getLogoUrl();
            settings.setLogoUrl(fileStorageService.storeLogo(dto.getLogoFile()));
            deleteStoredBranding(previous);
        } else {
            settings.setLogoUrl(trimToNull(dto.getLogoUrl()));
        }
        if (dto.getFaviconFile() != null && !dto.getFaviconFile().isEmpty()) {
            String previous = settings.getFaviconUrl();
            settings.setFaviconUrl(fileStorageService.storeFavicon(dto.getFaviconFile()));
            deleteStoredBranding(previous);
        } else {
            settings.setFaviconUrl(trimToNull(dto.getFaviconUrl()));
        }

        settings.setFacebookUrl(trimToNull(dto.getFacebookUrl()));
        settings.setTwitterUrl(trimToNull(dto.getTwitterUrl()));
        settings.setInstagramUrl(trimToNull(dto.getInstagramUrl()));
        settings.setLinkedinUrl(trimToNull(dto.getLinkedinUrl()));
        settings.setYoutubeUrl(trimToNull(dto.getYoutubeUrl()));
        settings.setDefaultRegistrationFee(dto.getDefaultRegistrationFee() == null
                ? BigDecimal.ZERO : dto.getDefaultRegistrationFee());
        settings.setCurrency(dto.getCurrency() == null || dto.getCurrency().isBlank() ? "INR" : dto.getCurrency().trim().toUpperCase(Locale.ROOT));
        settings.setDefaultDurationMonths(dto.getDefaultDurationMonths());
        settings.setDefaultLevel(dto.getDefaultLevel());
        settings.setDefaultTrainingMode(dto.getDefaultTrainingMode());
        settings.setDefaultCertificateEligible(dto.isDefaultCertificateEligible());
        settings.setDefaultPublicListed(dto.isDefaultPublicListed());
        settings.setPaymentGatewayLabel(trimToNull(dto.getPaymentGatewayLabel()));
        settings.setPaymentInstructions(trimToNull(dto.getPaymentInstructions()));
        settings.setAllowCoupons(dto.isAllowCoupons());
        settings.setEmailNotifications(dto.isEmailNotifications());
        settings.setWhatsappNotifications(dto.isWhatsappNotifications());
        settings.setSmsNotifications(dto.isSmsNotifications());
        settings.setCertificateSignatory(trimToNull(dto.getCertificateSignatory()));
        settings.setCertificateSignatoryTitle(trimToNull(dto.getCertificateSignatoryTitle()));
        settings.setCertificateFooterNote(trimToNull(dto.getCertificateFooterNote()));
        settings.setPrivacyPolicy(trimToNull(dto.getPrivacyPolicy()));
        settings.setTermsAndConditions(trimToNull(dto.getTermsAndConditions()));
        instituteSettingsRepository.save(settings);
    }

    /** Removes a previously uploaded branding asset (logo/favicon), ignoring external URLs. */
    private void deleteStoredBranding(String stored) {
        if (stored == null || stored.isBlank()) {
            return;
        }
        String value = toRelativeUpload(stored);
        if (value.startsWith("branding/")) {
            fileStorageService.deleteIfExists(value);
        }
    }

    /** Strips the /uploads/ prefix so a browser URL can be resolved back to a stored path. */
    private String toRelativeUpload(String url) {
        if (url == null) {
            return "";
        }
        String value = url.trim();
        if (value.startsWith("/uploads/")) {
            return value.substring("/uploads/".length());
        }
        return value;
    }

    /** Normalises a colour picker value to #RRGGBB, or null when blank. */
    private String normaliseHex(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT).replaceFirst("^#", "#");
    }

    // =====================================================================
    // Mapping + validation helpers
    // =====================================================================
    private Student requireStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
    }

    private Trainer requireTrainer(Long id) {
        return trainerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found."));
    }

    private Batch requireBatch(Long id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found."));
    }

    private LiveClass requireLiveClass(Long id) {
        return liveClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Live class not found."));
    }

    private Coupon requireCoupon(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found."));
    }

    private Announcement requireAnnouncement(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found."));
    }

    private void requireUniqueEmail(String email, Long allowedUserId) {
        userRepository.findByEmail(email.trim()).ifPresent(existing -> {
            if (allowedUserId == null || !existing.getId().equals(allowedUserId)) {
                throw new BusinessException("That email is already registered.");
            }
        });
    }

    private void requirePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new BusinessException("Password must be at least 6 characters.");
        }
    }

    private void applyUser(User user, String first, String last, String email, String phone, Role role) {
        user.setFirstName(first.trim());
        user.setLastName(last.trim());
        user.setEmail(email.trim().toLowerCase(Locale.ROOT));
        user.setPhone(trimToNull(phone));
        user.setRole(role);
    }

    private void applyBatch(Batch batch, BatchDto dto) {
        Course course = courseRepository.findByIdAndDeletedFalse(dto.getCourseId())
                .orElseThrow(() -> new BusinessException("Select a valid course."));
        Trainer trainer = requireTrainer(dto.getTrainerId());
        if (dto.getBatchName() == null || dto.getBatchName().isBlank()) {
            throw new BusinessException("Batch name is required.");
        }
        if (dto.getStartDate() != null && dto.getEndDate() != null
                && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BusinessException("End date cannot be before the start date.");
        }
        if (dto.getStartTime() != null && dto.getEndTime() != null
                && dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new BusinessException("End time cannot be before the start time.");
        }
        long enrolled = batch.getId() == null ? 0 : enrollmentRepository.countByBatch_Id(batch.getId());
        if (dto.getMaxSeats() != null && dto.getMaxSeats() < enrolled) {
            throw new BusinessException("Capacity cannot be lower than the " + enrolled
                    + " students already enrolled in this batch.");
        }
        TrainingMode mode = dto.getMode() == null ? TrainingMode.HYBRID : dto.getMode();
        if (mode != TrainingMode.ONLINE
                && (dto.getClassroomAddress() == null || dto.getClassroomAddress().isBlank())) {
            throw new BusinessException("Classroom address is required for offline and hybrid batches.");
        }
        if (mode != TrainingMode.OFFLINE
                && (dto.getMeetingLink() == null || dto.getMeetingLink().isBlank())) {
            throw new BusinessException("A meeting link is required for online and hybrid batches.");
        }
        batch.setBatchName(dto.getBatchName().trim());
        batch.setCourse(course);
        batch.setTrainer(trainer);
        batch.setStartDate(dto.getStartDate());
        batch.setEndDate(dto.getEndDate());
        batch.setStartTime(dto.getStartTime());
        batch.setEndTime(dto.getEndTime());
        batch.setMode(mode);
        batch.setStatus(dto.getStatus() == null ? ClassStatus.SCHEDULED : dto.getStatus());
        batch.setEnrollmentStatus(dto.getEnrollmentStatus() == null
                ? BatchEnrollmentStatus.OPEN : dto.getEnrollmentStatus());
        batch.setPublished(dto.isPublished());
        batch.setMaxSeats(dto.getMaxSeats());
        batch.setMeetingLink(trimToNull(dto.getMeetingLink()));
        batch.setClassroomAddress(trimToNull(dto.getClassroomAddress()));
        batch.setRoomNumber(trimToNull(dto.getRoomNumber()));
        batch.setNotes(trimToNull(dto.getNotes()));
    }

    private void applyLiveClass(LiveClass liveClass, LiveClassDto dto) {
        Batch batch = requireBatch(dto.getBatchId());
        liveClass.setTopic(dto.getTopic().trim());
        liveClass.setBatch(batch);
        liveClass.setStartTime(dto.getStartTime());
        liveClass.setEndTime(dto.getEndTime());
        liveClass.setMode(dto.getMode() == null ? ClassMode.ONLINE : dto.getMode());
        liveClass.setMeetingLink(trimToNull(dto.getMeetingLink()));
        liveClass.setClassroomDetails(trimToNull(dto.getClassroomDetails()));
        liveClass.setStatus(dto.getStatus() == null ? ClassStatus.SCHEDULED : dto.getStatus());
    }

    private void applyAnnouncement(Announcement announcement, AnnouncementDto dto) {
        announcement.setTitle(dto.getTitle().trim());
        announcement.setContent(dto.getContent().trim());
        if (dto.getBatchId() != null && dto.getBatchId() > 0) {
            announcement.setBatch(requireBatch(dto.getBatchId()));
        } else {
            announcement.setBatch(null);
        }
    }

    private boolean matches(User user, String q) {
        return fullName(user).toLowerCase(Locale.ROOT).contains(q)
                || (user.getEmail() != null && user.getEmail().toLowerCase(Locale.ROOT).contains(q))
                || (user.getPhone() != null && user.getPhone().contains(q));
    }

    private StudentDto toStudentDto(Student student) {
        User user = student.getUser();
        List<Enrollment> enrollments = enrollmentRepository.findByStudent_IdOrderByCreatedAtDesc(student.getId());
        long completed = enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED).count();
        return new StudentDto()
                .setId(student.getId())
                .setUserId(user.getId())
                .setFirstName(user.getFirstName())
                .setLastName(user.getLastName())
                .setFullName(fullName(user))
                .setEmail(user.getEmail())
                .setPhone(user.getPhone())
                .setEducation(student.getEducation())
                .setActive(user.isActive())
                .setEnrolledCourses(enrollments.size())
                .setCompletedCourses(completed)
                .setPaymentSummary(summarizePayments(student.getId()))
                .setCreatedAt(user.getCreatedAt());
    }

    private TrainerDto toTrainerDto(Trainer trainer) {
        User user = trainer.getUser();
        return new TrainerDto()
                .setId(trainer.getId())
                .setUserId(user.getId())
                .setFullName(fullName(user))
                .setFirstName(user.getFirstName())
                .setLastName(user.getLastName())
                .setExpertise(trainer.getExpertise())
                .setEmail(user.getEmail())
                .setPhone(user.getPhone())
                .setBio(trainer.getBio())
                .setExperienceYears(trainer.getExperienceYears())
                .setPhotoUrl(toPhotoUrl(trainer.getPhotoUrl()))
                .setActive(user.isActive())
                .setCourseCount(courseRepository.findByTrainer_IdAndDeletedFalseOrderByTitleAsc(trainer.getId()).size())
                .setBatchCount(batchRepository.findByTrainer_IdOrderByCreatedAtDesc(trainer.getId()).size())
                .setCreatedAt(trainer.getCreatedAt());
    }

    private BatchDto toBatchDto(Batch batch) {
        long enrolled = enrollmentRepository.countByBatch_Id(batch.getId());
        Integer max = batch.getMaxSeats();
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
                .setEnrollmentStatus(batch.getEnrollmentStatus())
                .setEnrollmentStatusLabel(label(batch.getEnrollmentStatus()))
                .setPublished(batch.isPublished())
                .setMaxSeats(max)
                .setMeetingLink(batch.getMeetingLink())
                .setClassroomAddress(batch.getClassroomAddress())
                .setRoomNumber(batch.getRoomNumber())
                .setNotes(batch.getNotes())
                .setEnrolledCount(enrolled)
                .setSeatsAvailable(max == null ? 0 : Math.max(0, max - enrolled));
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

    private EnrollmentDto toEnrollmentDto(Enrollment enrollment) {
        User studentUser = enrollment.getStudent().getUser();
        Course course = enrollment.getCourse();
        long total = lessonRepository.countByModule_Course_IdAndPublishedTrue(course.getId());
        long done = lessonCompletionRepository.countByStudent_IdAndLesson_Module_Course_Id(
                enrollment.getStudent().getId(), course.getId());
        int percent = total == 0 ? 0 : (int) Math.round(Math.min(done, total) * 100.0 / total);
        EnrollmentDto dto = new EnrollmentDto()
                .setId(enrollment.getId())
                .setStudentId(enrollment.getStudent().getId())
                .setStudentName(fullName(studentUser))
                .setStudentEmail(studentUser.getEmail())
                .setCourseId(course.getId())
                .setCourseTitle(course.getTitle())
                .setStatus(enrollment.getStatus())
                .setStatusLabel(label(enrollment.getStatus()))
                .setTrainingMode(enrollment.getTrainingMode())
                .setTrainingModeLabel(enrollment.getTrainingMode() == null ? null : label(enrollment.getTrainingMode()))
                .setAmount(enrollment.getAmount())
                .setAmountLabel(enrollment.getAmount() == null ? null : formatInr(enrollment.getAmount()))
                .setPaymentStatus(enrollment.getPaymentStatus())
                .setPaymentStatusLabel(enrollment.getPaymentStatus() == null ? null : label(enrollment.getPaymentStatus()))
                .setCompletionPercent(enrollment.getStatus() == EnrollmentStatus.COMPLETED ? 100 : percent)
                .setEnrolledAt(enrollment.getCreatedAt());
        if (enrollment.getBatch() != null) {
            dto.setBatchId(enrollment.getBatch().getId())
                    .setBatchName(enrollment.getBatch().getBatchName());
        }
        return dto;
    }

    private CouponDto toCouponDto(Coupon coupon) {
        boolean expired = coupon.hasExpired();
        boolean exhausted = coupon.isExhausted();
        String status = !coupon.isActive() ? "Inactive"
                : (!coupon.hasStarted() ? "Not started"
                : (expired ? "Expired" : (exhausted ? "Exhausted" : "Active")));
        String summary = coupon.getDiscountType() == com.futureboundtech.enums.CouponDiscountType.PERCENTAGE
                ? trimNumber(coupon.getDiscountPercentage()) + "% off"
                    + (coupon.getMaxDiscount() != null ? " (up to " + formatInr(coupon.getMaxDiscount()) + ")" : "")
                : formatInr(coupon.getDiscountAmount()) + " off";
        CouponDto dto = new CouponDto()
                .setId(coupon.getId())
                .setCode(coupon.getCode())
                .setDiscountAmount(coupon.getDiscountAmount())
                .setDiscountType(coupon.getDiscountType())
                .setDiscountPercentage(coupon.getDiscountPercentage())
                .setMaxDiscount(coupon.getMaxDiscount())
                .setValidFrom(coupon.getValidFrom())
                .setValidUntil(coupon.getValidUntil())
                .setUsageLimit(coupon.getUsageLimit())
                .setUsedCount(coupon.getUsedCount())
                .setActive(coupon.isActive())
                .setFormattedDiscount(formatInr(coupon.getDiscountAmount()))
                .setDiscountSummary(summary)
                .setStatusLabel(status)
                .setCreatedAt(coupon.getCreatedAt());
        dto.setApplicableCourseIds(coupon.getApplicableCourseIds() == null
                ? new ArrayList<>() : new ArrayList<>(coupon.getApplicableCourseIds()));
        return dto;
    }

    private String trimNumber(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NameValueDto> listCourseOptions() {
        return courseRepository.findAll().stream()
                .filter(c -> !c.isDeleted())
                .map(c -> new NameValueDto(c.getTitle(), c.getId()))
                .sorted(Comparator.comparing(NameValueDto::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    private AnnouncementDto toAnnouncementDto(Announcement announcement) {
        boolean global = announcement.getBatch() == null;
        return new AnnouncementDto()
                .setId(announcement.getId())
                .setTitle(announcement.getTitle())
                .setContent(announcement.getContent())
                .setBatchId(global ? null : announcement.getBatch().getId())
                .setBatchName(global ? "All students" : announcement.getBatch().getBatchName())
                .setGlobal(global)
                .setCreatedAt(announcement.getCreatedAt());
    }

    private ContactMessageDto toContactDto(ContactMessage message) {
        ContactStatus status = message.getStatus() == null ? ContactStatus.NEW : message.getStatus();
        return new ContactMessageDto()
                .setId(message.getId())
                .setName(message.getName())
                .setEmail(message.getEmail())
                .setPhone(message.getPhone())
                .setCourseInterest(message.getCourseInterest())
                .setSubject(message.getSubject())
                .setMessage(message.getMessage())
                .setReplied(message.isReplied())
                .setStatus(status)
                .setStatusLabel(status.getLabel())
                .setCreatedAt(message.getCreatedAt());
    }

    private CertificateDto toCertificateDto(Certificate certificate) {
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
                .setStatusLabel(label(certificate.getStatus()))
                .setRevokeReason(certificate.getRevokeReason())
                .setIssuedBy(certificate.getIssuedBy());
    }

    private ReviewDto toReviewDto(Review review) {
        String studentName = review.getStudent() != null && review.getStudent().getUser() != null
                ? fullName(review.getStudent().getUser()) : null;
        String courseTitle = review.getCourse() != null ? review.getCourse().getTitle() : null;
        String author = review.getAuthorName() != null && !review.getAuthorName().isBlank()
                ? review.getAuthorName() : studentName;
        String context = review.getContextLabel() != null && !review.getContextLabel().isBlank()
                ? review.getContextLabel() : courseTitle;
        return new ReviewDto()
                .setId(review.getId())
                .setStudentName(studentName)
                .setCourseTitle(courseTitle)
                .setAuthorName(author)
                .setContextLabel(context)
                .setRating(review.getRating())
                .setComment(review.getComment())
                .setApproved(review.isApproved())
                .setDemo(review.isDemo())
                .setPublished(review.isPublished())
                .setStatusLabel(review.isPublished() ? "Published" : (review.isApproved() ? "Approved" : "Pending"))
                .setCreatedAt(review.getCreatedAt());
    }

    private PaymentDto toPaymentDto(Payment payment) {
        return new PaymentDto()
                .setId(payment.getId())
                .setReceiptNumber(payment.getReceiptNumber())
                .setCourseTitle(payment.getCourse().getTitle())
                .setStudentName(fullName(payment.getStudent().getUser()))
                .setAmount(payment.getAmount())
                .setFormattedAmount(formatInr(payment.getAmount()))
                .setStatus(payment.getStatus())
                .setStatusLabel(label(payment.getStatus()))
                .setPaymentMethod(payment.getPaymentMethod())
                .setCreatedAt(payment.getCreatedAt())
                .setPaidAt(payment.getPaidAt());
    }

    private UserBriefDto toUserBrief(User user) {
        return new UserBriefDto()
                .setId(user.getId())
                .setFullName(fullName(user))
                .setEmail(user.getEmail())
                .setRole(user.getRole().name())
                .setRoleLabel(label(user.getRole()))
                .setActive(user.isActive())
                .setCreatedAt(user.getCreatedAt());
    }

    private String summarizePayments(Long studentId) {
        BigDecimal collected = paymentRepository.findByStudent_IdOrderByCreatedAtDesc(studentId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return collected.compareTo(BigDecimal.ZERO) > 0 ? formatInr(collected) : "No payments";
    }

    private String fullName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
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

    private String formatInr(BigDecimal amount) {
        if (amount == null) {
            return formatInr(BigDecimal.ZERO);
        }
        NumberFormat format = NumberFormat.getCurrencyInstance(INR_LOCALE);
        format.setMaximumFractionDigits(2);
        return format.format(amount);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
