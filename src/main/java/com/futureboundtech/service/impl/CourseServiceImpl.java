package com.futureboundtech.service.impl;

import com.futureboundtech.dto.CourseDto;
import com.futureboundtech.dto.BatchDto;
import com.futureboundtech.dto.TrainerDto;
import com.futureboundtech.entity.Batch;
import com.futureboundtech.entity.Course;
import com.futureboundtech.entity.Enrollment;
import com.futureboundtech.entity.Student;
import com.futureboundtech.entity.Trainer;
import com.futureboundtech.entity.User;
import com.futureboundtech.enums.BatchEnrollmentStatus;
import com.futureboundtech.enums.CourseCategory;
import com.futureboundtech.enums.CourseLevel;
import com.futureboundtech.enums.CourseStatus;
import com.futureboundtech.enums.EnrollmentStatus;
import com.futureboundtech.enums.Role;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.exception.ResourceNotFoundException;
import com.futureboundtech.repository.BatchRepository;
import com.futureboundtech.repository.CourseRepository;
import com.futureboundtech.repository.EnrollmentRepository;
import com.futureboundtech.repository.PaymentRepository;
import com.futureboundtech.repository.QuizRepository;
import com.futureboundtech.repository.ReviewRepository;
import com.futureboundtech.repository.StudentRepository;
import com.futureboundtech.repository.TrainerRepository;
import com.futureboundtech.service.CourseService;
import com.futureboundtech.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final TrainerRepository trainerRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final BatchRepository batchRepository;
    private final PaymentRepository paymentRepository;
    private final QuizRepository quizRepository;
    private final ReviewRepository reviewRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public List<CourseDto> searchPublic(String query, CourseCategory category, CourseLevel level) {
        return courseRepository.searchPublicCatalog(normalizeQuery(query), category, level, CourseStatus.PUBLISHED)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseDto> findFeaturedPublic(int limit) {
        return searchPublic(null, null, null).stream()
                .limit(Math.max(limit, 0))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDto findPublicBySlug(String slug) {
        Course course = courseRepository.findBySlugAndDeletedFalse(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found."));
        if (course.getStatus() != CourseStatus.PUBLISHED || !course.isPublicListed()) {
            throw new ResourceNotFoundException("Course not found.");
        }
        return toDto(course);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseDto> searchAdmin(String query, CourseCategory category, CourseLevel level, CourseStatus status) {
        return courseRepository.searchAdmin(normalizeQuery(query), category, level, status)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDto findAdminById(Long id) {
        return findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDto findById(Long id) {
        return toDto(requireCourse(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseDto> findCoursesForTrainer(User trainerUser) {
        return trainerRepository.findByUser_Id(trainerUser.getId())
                .map(trainer -> courseRepository.findByTrainer_IdAndDeletedFalseOrderByTitleAsc(trainer.getId()))
                .orElse(List.of())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CourseDto create(CourseDto dto, MultipartFile thumbnail) {
        validateFees(dto);
        Course course = new Course();
        applyDto(course, dto, true);
        String stored = fileStorageService.storeCourseThumbnail(thumbnail);
        if (stored != null) {
            course.setThumbnailPath(stored);
        }
        return toDto(courseRepository.save(course));
    }

    @Override
    @Transactional
    public CourseDto update(Long id, CourseDto dto, MultipartFile thumbnail) {
        validateFees(dto);
        Course course = requireCourse(id);
        applyDto(course, dto, false);
        if (thumbnail != null && !thumbnail.isEmpty()) {
            String previous = course.getThumbnailPath();
            course.setThumbnailPath(fileStorageService.storeCourseThumbnail(thumbnail));
            fileStorageService.deleteIfExists(previous);
        }
        return toDto(courseRepository.save(course));
    }

    @Override
    @Transactional
    public void publish(Long id) {
        Course course = requireCourse(id);
        course.setStatus(CourseStatus.PUBLISHED);
        course.setPublicListed(true);
        courseRepository.save(course);
    }

    @Override
    @Transactional
    public void unpublish(Long id) {
        Course course = requireCourse(id);
        course.setStatus(CourseStatus.UNPUBLISHED);
        course.setPublicListed(false);
        courseRepository.save(course);
    }

    @Override
    @Transactional
    public void deleteSafely(Long id) {
        Course course = requireCourse(id);
        long enrollments = enrollmentRepository.countByCourse_Id(id);
        long batches = batchRepository.countByCourse_Id(id);
        long payments = paymentRepository.countByCourse_Id(id);
        long quizzes = quizRepository.countByCourse_Id(id);
        long reviews = reviewRepository.countByCourse_Id(id);

        if (enrollments > 0 || batches > 0 || payments > 0 || quizzes > 0 || reviews > 0) {
            throw new BusinessException(
                    "This course cannot be deleted because it has related records ("
                            + enrollments + " enrollments, "
                            + batches + " batches, "
                            + payments + " payments, "
                            + quizzes + " quizzes, "
                            + reviews + " reviews). Unpublish it instead to hide it from the catalog.");
        }

        course.setDeleted(true);
        course.setStatus(CourseStatus.UNPUBLISHED);
        course.setPublicListed(false);
        courseRepository.save(course);
    }

    @Override
    @Transactional
    public void enrollStudent(String slug, User currentUser) {
        enrollStudent(slug, currentUser, null);
    }

    @Override
    @Transactional
    public void enrollStudent(String slug, User currentUser, Long batchId) {
        if (currentUser == null) {
            throw new BusinessException("Please log in to enroll.");
        }
        if (currentUser.getRole() != Role.STUDENT) {
            throw new BusinessException("Only student accounts can enroll in a course.");
        }
        Course course = courseRepository.findBySlugAndDeletedFalse(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found."));
        if (course.getStatus() != CourseStatus.PUBLISHED || !course.isPublicListed()) {
            throw new BusinessException("This course is not open for enrollment.");
        }

        Student student = studentRepository.findByUser_Id(currentUser.getId())
                .orElseThrow(() -> new BusinessException("Student profile is missing. Please contact support."));

        if (enrollmentRepository.existsByStudent_IdAndCourse_Id(student.getId(), course.getId())) {
            throw new BusinessException("You are already enrolled in this course.");
        }

        Batch batch = null;
        if (batchId != null) {
            batch = requireEnrollableBatch(course, batchId, student.getId());
        }

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .batch(batch)
                .trainingMode(batch != null && batch.getMode() != null ? batch.getMode() : course.getTrainingMode())
                .amount(java.math.BigDecimal.ZERO)
                .paymentStatus(com.futureboundtech.enums.PaymentStatus.SUCCESS)
                .status(EnrollmentStatus.ACTIVE)
                .build();
        enrollmentRepository.save(enrollment);
    }

    /** Validates that the batch belongs to the course, is published, open and has seats left. */
    private Batch requireEnrollableBatch(Course course, Long batchId, Long studentId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException("Select a valid batch."));
        if (!batch.getCourse().getId().equals(course.getId())) {
            throw new BusinessException("The selected batch does not belong to this course.");
        }
        if (!batch.isPublished()) {
            throw new BusinessException("The selected batch is not open for enrollment.");
        }
        if (batch.getEnrollmentStatus() == BatchEnrollmentStatus.CLOSED) {
            throw new BusinessException("Enrollment for the selected batch is closed.");
        }
        if (batch.getMaxSeats() != null) {
            long enrolled = enrollmentRepository.countSeatsHeldByBatch(batch.getId());
            if (enrolled >= batch.getMaxSeats()) {
                throw new BusinessException("The selected batch is full. Please choose another batch.");
            }
        }
        return batch;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchDto> findEnrollableBatches(String slug) {
        Course course = courseRepository.findBySlugAndDeletedFalse(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found."));
        return batchRepository.findByCourse_IdOrderByCreatedAtDesc(course.getId()).stream()
                .filter(Batch::isPublished)
                .filter(b -> b.getEnrollmentStatus() != BatchEnrollmentStatus.CLOSED)
                .map(this::toBatchOption)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchDto> findPublicEnrollableBatches() {
        return batchRepository.findByPublishedTrueOrderByStartDateAscIdAsc().stream()
                .filter(b -> b.getEnrollmentStatus() != BatchEnrollmentStatus.CLOSED)
                .filter(b -> b.getCourse() != null && b.getCourse().getStatus() == CourseStatus.PUBLISHED)
                .map(this::toBatchOption)
                .collect(Collectors.toList());
    }

    private BatchDto toBatchOption(Batch batch) {
        long enrolled = enrollmentRepository.countSeatsHeldByBatch(batch.getId());
        Integer max = batch.getMaxSeats();
        return new BatchDto()
                .setId(batch.getId())
                .setBatchName(batch.getBatchName())
                .setCourseId(batch.getCourse().getId())
                .setCourseTitle(batch.getCourse().getTitle())
                .setCourseSlug(batch.getCourse().getSlug())
                .setTrainerId(batch.getTrainer().getId())
                .setTrainerName(fullName(batch.getTrainer().getUser()))
                .setStartDate(batch.getStartDate())
                .setEndDate(batch.getEndDate())
                .setStartTime(batch.getStartTime())
                .setEndTime(batch.getEndTime())
                .setMode(batch.getMode())
                .setStatus(batch.getStatus())
                .setEnrollmentStatus(batch.getEnrollmentStatus())
                .setMaxSeats(max)
                .setEnrolledCount(enrolled)
                .setSeatsAvailable(max == null ? 0 : Math.max(0, max - enrolled));
    }

    private String fullName(User user) {
        if (user == null) {
            return "";
        }
        return ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
    }


    @Override
    @Transactional(readOnly = true)
    public boolean isStudentEnrolled(String slug, User currentUser) {
        if (currentUser == null || currentUser.getRole() != Role.STUDENT) {
            return false;
        }
        return studentRepository.findByUser_Id(currentUser.getId())
                .flatMap(student -> courseRepository.findBySlugAndDeletedFalse(slug)
                        .map(course -> enrollmentRepository.existsByStudent_IdAndCourse_Id(student.getId(), course.getId())))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainerDto> listTrainers() {
        return trainerRepository.findAllWithUser().stream()
                .map(this::toTrainerDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return courseRepository.countByDeletedFalse();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPublished() {
        return courseRepository.countByDeletedFalseAndStatus(CourseStatus.PUBLISHED);
    }

    private Course requireCourse(Long id) {
        return courseRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found."));
    }

    private void applyDto(Course course, CourseDto dto, boolean creating) {
        course.setTitle(dto.getTitle().trim());
        course.setSlug(uniqueSlug(dto.getSlug(), dto.getTitle(), creating ? null : course.getId()));
        course.setShortDescription(dto.getShortDescription().trim());
        course.setDescription(dto.getDetailedDescription().trim());
        course.setCategory(dto.getCategory());
        course.setLevel(dto.getLevel());
        course.setDurationInDays(dto.getDurationMonths() * 30);
        course.setFee(dto.getFee() == null ? BigDecimal.ZERO : dto.getFee());
        course.setDiscountFee(isBlankMoney(dto.getDiscountFee()) ? null : dto.getDiscountFee());
        course.setTrainingMode(dto.getTrainingMode());
        course.setStatus(dto.getStatus());
        course.setLearningOutcomes(trimToNull(dto.getLearningOutcomes()));
        course.setPrerequisites(trimToNull(dto.getPrerequisites()));
        course.setProjects(trimToNull(dto.getProjects()));
        course.setCertificateEligible(dto.isCertificateEligible());
        course.setPublicListed(dto.isPublicListed() && dto.getStatus() == CourseStatus.PUBLISHED);
        if (dto.getStatus() != CourseStatus.PUBLISHED) {
            course.setPublicListed(false);
        }
        course.setTrainer(resolveTrainer(dto.getTrainerId()));
    }

    private Trainer resolveTrainer(Long trainerId) {
        if (trainerId == null) {
            return null;
        }
        return trainerRepository.findById(trainerId)
                .orElseThrow(() -> new BusinessException("Selected trainer was not found."));
    }

    private void validateFees(CourseDto dto) {
        BigDecimal fee = dto.getFee() == null ? BigDecimal.ZERO : dto.getFee();
        BigDecimal discount = dto.getDiscountFee();
        if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            if (fee.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Set a course fee before adding a discount fee.");
            }
            if (discount.compareTo(fee) >= 0) {
                throw new BusinessException("Discount fee must be lower than the course fee.");
            }
        }
    }

    private String uniqueSlug(String requested, String title, Long currentId) {
        String base = toSlug(requested == null || requested.isBlank() ? title : requested);
        if (base.isBlank()) {
            throw new BusinessException("A valid slug could not be generated from the title.");
        }
        String candidate = base;
        int suffix = 2;
        while (slugTaken(candidate, currentId)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private boolean slugTaken(String slug, Long currentId) {
        if (currentId == null) {
            return courseRepository.existsBySlugAndDeletedFalse(slug);
        }
        return courseRepository.existsBySlugAndDeletedFalseAndIdNot(slug, currentId);
    }

    private String toSlug(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private CourseDto toDto(Course course) {
        int months = Math.max(1, (int) Math.round(course.getDurationInDays() / 30.0));
        CourseDto dto = new CourseDto()
                .setId(course.getId())
                .setTitle(course.getTitle())
                .setSlug(course.getSlug())
                .setShortDescription(course.getShortDescription())
                .setDetailedDescription(course.getDescription())
                .setThumbnailPath(course.getThumbnailPath())
                .setThumbnailUrl(toThumbnailUrl(course.getThumbnailPath()))
                .setCategory(course.getCategory())
                .setLevel(course.getLevel())
                .setDurationMonths(months)
                .setDurationLabel(months + (months == 1 ? " Month" : " Months"))
                .setFee(isBlankMoney(course.getFee()) ? null : course.getFee())
                .setDiscountFee(course.getDiscountFee())
                .setTrainingMode(course.getTrainingMode())
                .setStatus(course.getStatus())
                .setLearningOutcomes(course.getLearningOutcomes())
                .setPrerequisites(course.getPrerequisites())
                .setProjects(course.getProjects())
                .setCertificateEligible(course.isCertificateEligible())
                .setPublicListed(course.isPublicListed())
                .setThemeKey(resolveThemeKey(course.getSlug(), course.getTitle()))
                .setFeeConfigured(course.hasConfiguredFee())
                .setDiscountConfigured(course.hasDiscount())
                .setFormattedFee(formatInr(course.getFee()))
                .setFormattedDiscountFee(formatInr(course.getDiscountFee()));

        if (course.getTrainer() != null && course.getTrainer().getUser() != null) {
            User trainerUser = course.getTrainer().getUser();
            dto.setTrainerId(course.getTrainer().getId());
            dto.setTrainerName(trainerUser.getFirstName() + " " + trainerUser.getLastName());
            dto.setTrainerExpertise(course.getTrainer().getExpertise());
        }
        return dto;
    }

    private TrainerDto toTrainerDto(Trainer trainer) {
        User user = trainer.getUser();
        return new TrainerDto()
                .setId(trainer.getId())
                .setFullName(user.getFirstName() + " " + user.getLastName())
                .setExpertise(trainer.getExpertise())
                .setEmail(user.getEmail());
    }

    private String toThumbnailUrl(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return "/uploads/" + path.replace("\\", "/");
    }

    private String resolveThemeKey(String slug, String title) {
        String haystack = ((slug == null ? "" : slug) + " " + (title == null ? "" : title)).toLowerCase(Locale.ROOT);
        if (haystack.contains("java")) {
            return "java";
        }
        if (haystack.contains("python")) {
            return "python";
        }
        if (haystack.contains("aws") || haystack.contains("cloud")) {
            return "aws";
        }
        if (haystack.contains("artificial") || haystack.contains("ai")) {
            return "ai";
        }
        return "web";
    }

    private String formatInr(BigDecimal amount) {
        if (isBlankMoney(amount)) {
            return null;
        }
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        format.setMaximumFractionDigits(0);
        return format.format(amount);
    }

    private boolean isBlankMoney(BigDecimal amount) {
        return amount == null || amount.compareTo(BigDecimal.ZERO) <= 0;
    }

    private String normalizeQuery(String query) {
        return query == null ? null : query.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
