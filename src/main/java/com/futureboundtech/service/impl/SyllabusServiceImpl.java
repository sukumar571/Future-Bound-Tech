package com.futureboundtech.service.impl;

import com.futureboundtech.dto.CourseModuleDto;
import com.futureboundtech.dto.CourseProgressDto;
import com.futureboundtech.dto.LessonDto;
import com.futureboundtech.dto.LessonResourceDto;
import com.futureboundtech.entity.Course;
import com.futureboundtech.entity.CourseModule;
import com.futureboundtech.entity.Lesson;
import com.futureboundtech.entity.LessonCompletion;
import com.futureboundtech.entity.LessonResource;
import com.futureboundtech.entity.Student;
import com.futureboundtech.entity.User;
import com.futureboundtech.enums.EnrollmentStatus;
import com.futureboundtech.enums.Role;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.exception.ResourceNotFoundException;
import com.futureboundtech.repository.CourseModuleRepository;
import com.futureboundtech.repository.CourseRepository;
import com.futureboundtech.repository.EnrollmentRepository;
import com.futureboundtech.repository.LessonCompletionRepository;
import com.futureboundtech.repository.LessonRepository;
import com.futureboundtech.repository.LessonResourceRepository;
import com.futureboundtech.repository.StudentRepository;
import com.futureboundtech.security.CustomUserDetails;
import com.futureboundtech.service.FileStorageService;
import com.futureboundtech.service.SyllabusService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SyllabusServiceImpl implements SyllabusService {

    private static final List<String> RESOURCE_TYPES = List.of("NOTES", "SLIDES", "CODE", "REFERENCE", "OTHER");

    private static final Comparator<CourseModule> MODULE_ORDER =
            Comparator.comparing(CourseModule::getOrderIndex, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(CourseModule::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<Lesson> LESSON_ORDER =
            Comparator.comparing(Lesson::getOrderIndex, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Lesson::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final LessonResourceRepository resourceRepository;
    private final LessonCompletionRepository completionRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final FileStorageService fileStorageService;

    // ================= Admin / Trainer management =================

    @Override
    @Transactional(readOnly = true)
    public List<CourseModuleDto> getAdminSyllabus(Long courseId) {
        requireManageableCourse(courseId);
        return modulesWithLessons(courseId).stream()
                .map(this::toAdminModuleDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseModuleDto getModuleForAdmin(Long moduleId) {
        CourseModule module = requireModule(moduleId);
        CourseModuleDto dto = new CourseModuleDto()
                .setId(module.getId())
                .setTitle(module.getTitle())
                .setDescription(module.getDescription())
                .setOrderIndex(module.getOrderIndex());
        applyCourseInfo(dto, module);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public LessonDto getLessonForEdit(Long lessonId) {
        return toLessonDto(requireLesson(lessonId), true);
    }

    @Override
    @Transactional
    public CourseModuleDto createModule(Long courseId, CourseModuleDto dto) {
        Course course = requireManageableCourse(courseId);
        CourseModule module = new CourseModule();
        module.setTitle(requireTrimmed(dto.getTitle(), "Module title is required."));
        module.setDescription(trimToNull(dto.getDescription()));
        module.setOrderIndex(nextModuleOrder(courseId));
        module.setCourse(course);
        return toModuleSummary(moduleRepository.save(module));
    }

    @Override
    @Transactional
    public CourseModuleDto updateModule(Long moduleId, CourseModuleDto dto) {
        CourseModule module = requireModule(moduleId);
        module.setTitle(requireTrimmed(dto.getTitle(), "Module title is required."));
        module.setDescription(trimToNull(dto.getDescription()));
        return toModuleSummary(moduleRepository.save(module));
    }

    @Override
    @Transactional
    public Long deleteModule(Long moduleId) {
        CourseModule module = requireModule(moduleId);
        Long courseId = module.getCourse().getId();
        for (Lesson lesson : module.getLessons()) {
            deleteLessonFiles(lesson);
        }
        moduleRepository.delete(module);
        return courseId;
    }

    @Override
    @Transactional
    public Long moveModule(Long moduleId, int direction) {
        CourseModule module = requireModule(moduleId);
        Long courseId = module.getCourse().getId();
        List<CourseModule> modules = modulesWithLessons(courseId);
        int index = modules.indexOf(module);
        int target = index + direction;
        if (index >= 0 && target >= 0 && target < modules.size()) {
            Collections.swap(modules, index, target);
            for (int i = 0; i < modules.size(); i++) {
                modules.get(i).setOrderIndex(i + 1);
            }
            moduleRepository.saveAll(modules);
        }
        return courseId;
    }

    @Override
    @Transactional
    public LessonDto createLesson(Long moduleId, LessonDto dto) {
        CourseModule module = requireModule(moduleId);
        Lesson lesson = new Lesson();
        applyLessonDto(lesson, dto);
        lesson.setOrderIndex(nextLessonOrder(moduleId));
        lesson.setModule(module);
        return toLessonDto(lessonRepository.save(lesson), false);
    }

    @Override
    @Transactional
    public LessonDto updateLesson(Long lessonId, LessonDto dto) {
        Lesson lesson = requireLesson(lessonId);
        applyLessonDto(lesson, dto);
        return toLessonDto(lessonRepository.save(lesson), false);
    }

    @Override
    @Transactional
    public Long deleteLesson(Long lessonId) {
        Lesson lesson = requireLesson(lessonId);
        Long courseId = lesson.getModule().getCourse().getId();
        deleteLessonFiles(lesson);
        lessonRepository.delete(lesson);
        return courseId;
    }

    @Override
    @Transactional
    public Long moveLesson(Long lessonId, int direction) {
        Lesson lesson = requireLesson(lessonId);
        Long moduleId = lesson.getModule().getId();
        Long courseId = lesson.getModule().getCourse().getId();
        List<Lesson> lessons = lessonRepository.findByModule_IdOrderByOrderIndexAsc(moduleId);
        lessons.sort(LESSON_ORDER);
        int index = lessons.indexOf(lesson);
        int target = index + direction;
        if (index >= 0 && target >= 0 && target < lessons.size()) {
            Collections.swap(lessons, index, target);
            for (int i = 0; i < lessons.size(); i++) {
                lessons.get(i).setOrderIndex(i + 1);
            }
            lessonRepository.saveAll(lessons);
        }
        return courseId;
    }

    @Override
    @Transactional
    public LessonDto setLessonPublished(Long lessonId, boolean published) {
        Lesson lesson = requireLesson(lessonId);
        lesson.setPublished(published);
        return toLessonDto(lessonRepository.save(lesson), false);
    }

    @Override
    @Transactional
    public LessonResourceDto addResource(Long lessonId, LessonResourceDto dto, MultipartFile file) {
        Lesson lesson = requireLesson(lessonId);
        String type = dto.getResourceType() == null ? "" : dto.getResourceType().trim().toUpperCase(Locale.ROOT);
        if (!RESOURCE_TYPES.contains(type)) {
            throw new BusinessException("Select a valid resource type.");
        }
        String url;
        boolean stored;
        if (file != null && !file.isEmpty()) {
            url = fileStorageService.storeLessonResource(file);
            stored = true;
        } else if (dto.getFileUrl() != null && !dto.getFileUrl().isBlank()) {
            url = dto.getFileUrl().trim();
            String lower = url.toLowerCase(Locale.ROOT);
            if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
                throw new BusinessException("External link must start with http:// or https://.");
            }
            stored = false;
        } else {
            throw new BusinessException("Upload a file or provide an external link for the resource.");
        }
        LessonResource resource = LessonResource.builder()
                .title(requireTrimmed(dto.getTitle(), "Resource title is required."))
                .resourceType(type)
                .fileUrl(url)
                .lesson(lesson)
                .build();
        return toResourceDto(resourceRepository.save(resource));
    }

    @Override
    @Transactional
    public Long deleteResource(Long resourceId) {
        LessonResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found."));
        Long lessonId = resource.getLesson().getId();
        if (isStoredFile(resource.getFileUrl())) {
            fileStorageService.deleteIfExists(resource.getFileUrl());
        }
        resourceRepository.delete(resource);
        return lessonId;
    }

    @Override
    public List<String> getResourceTypes() {
        return RESOURCE_TYPES;
    }

    // ================= Student learning =================

    @Override
    @Transactional(readOnly = true)
    public List<CourseModuleDto> getStudentSyllabus(Long courseId, User user) {
        requireCourse(courseId);
        requireEnrollment(courseId, user);
        Set<Long> completedIds = completedLessonIds(courseId, user);
        return modulesWithLessons(courseId).stream()
                .map(module -> toStudentModuleDto(module, completedIds))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LessonDto getStudentLesson(Long lessonId, User user) {
        Lesson lesson = requireAccessibleLesson(lessonId, user);
        Student student = requireStudent(user);
        LessonDto dto = toLessonDto(lesson, true);
        dto.setCompleted(completionRepository.existsByStudent_IdAndLesson_Id(student.getId(), lessonId));
        return dto;
    }

    @Override
    @Transactional
    public void setLessonCompleted(Long lessonId, User user, boolean completed) {
        Lesson lesson = requireAccessibleLesson(lessonId, user);
        Student student = requireStudent(user);
        if (completed) {
            if (!completionRepository.existsByStudent_IdAndLesson_Id(student.getId(), lesson.getId())) {
                completionRepository.save(LessonCompletion.builder()
                        .student(student)
                        .lesson(lesson)
                        .completedAt(LocalDateTime.now())
                        .build());
            }
        } else {
            completionRepository.deleteByStudent_IdAndLesson_Id(student.getId(), lesson.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CourseProgressDto getCourseProgress(Long courseId, User user) {
        requireEnrollment(courseId, user);
        Student student = requireStudent(user);
        long total = lessonRepository.countByModule_Course_IdAndPublishedTrue(courseId);
        long completed = completionRepository.countByStudent_IdAndLesson_Module_Course_Id(student.getId(), courseId);
        int done = (int) Math.min(completed, total);
        int percent = total == 0 ? 0 : (int) Math.round(done * 100.0 / total);
        return new CourseProgressDto()
                .setCourseId(courseId)
                .setTotalLessons((int) total)
                .setCompletedLessons(done)
                .setPercent(percent);
    }

    // ================= Public catalog =================

    @Override
    @Transactional(readOnly = true)
    public List<CourseModuleDto> getPublicPreview(Long courseId) {
        return modulesWithLessons(courseId).stream()
                .map(module -> {
                    List<LessonDto> lessons = module.getLessons().stream()
                            .filter(Lesson::isPublished)
                            .sorted(LESSON_ORDER)
                            .map(lesson -> toLessonDto(lesson, false))
                            .collect(Collectors.toList());
                    CourseModuleDto dto = baseModuleDto(module)
                            .setLessons(lessons)
                            .setLessonCount(lessons.size())
                            .setPublishedLessonCount(lessons.size());
                    applyCourseInfo(dto, module);
                    return dto;
                })
                .filter(dto -> !dto.getLessons().isEmpty())
                .collect(Collectors.toList());
    }

    // ================= Resource download =================

    @Override
    @Transactional(readOnly = true)
    public LessonResource authorizeResource(Long lessonId, Long resourceId, User user) {
        requireAccessibleLesson(lessonId, user);
        LessonResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found."));
        if (resource.getLesson() == null || !lessonId.equals(resource.getLesson().getId())) {
            throw new ResourceNotFoundException("Resource not found.");
        }
        return resource;
    }

    @Override
    @Transactional(readOnly = true)
    public LessonResource authorizeResourcePreview(Long lessonId, Long resourceId, User user) {
        // requireLesson -> assertCanManage allows ADMIN, or the TRAINER who owns the course.
        requireLesson(lessonId);
        LessonResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found."));
        if (resource.getLesson() == null || !lessonId.equals(resource.getLesson().getId())) {
            throw new ResourceNotFoundException("Resource not found.");
        }
        return resource;
    }

    @Override
    public Resource loadStoredResourceFile(LessonResource resource) {
        return fileStorageService.loadAsResource(resource.getFileUrl());
    }

    // ================= Mapping helpers =================

    private CourseModuleDto toAdminModuleDto(CourseModule module) {
        List<LessonDto> lessons = module.getLessons().stream()
                .sorted(LESSON_ORDER)
                .map(lesson -> toLessonDto(lesson, false))
                .collect(Collectors.toList());
        CourseModuleDto dto = baseModuleDto(module)
                .setLessons(lessons)
                .setLessonCount(lessons.size())
                .setPublishedLessonCount((int) lessons.stream().filter(LessonDto::isPublished).count());
        applyCourseInfo(dto, module);
        return dto;
    }

    private CourseModuleDto toStudentModuleDto(CourseModule module, Set<Long> completedIds) {
        List<LessonDto> lessons = module.getLessons().stream()
                .filter(Lesson::isPublished)
                .sorted(LESSON_ORDER)
                .map(lesson -> toLessonDto(lesson, false)
                        .setCompleted(completedIds.contains(lesson.getId())))
                .collect(Collectors.toList());
        int completedCount = (int) lessons.stream().filter(LessonDto::isCompleted).count();
        CourseModuleDto dto = baseModuleDto(module)
                .setLessons(lessons)
                .setLessonCount(lessons.size())
                .setPublishedLessonCount(lessons.size())
                .setCompletedLessonCount(completedCount)
                .setProgressPercent(lessons.isEmpty() ? 0
                        : (int) Math.round(completedCount * 100.0 / lessons.size()));
        applyCourseInfo(dto, module);
        return dto;
    }

    private CourseModuleDto toModuleSummary(CourseModule module) {
        CourseModuleDto dto = new CourseModuleDto()
                .setId(module.getId())
                .setTitle(module.getTitle())
                .setOrderIndex(module.getOrderIndex());
        applyCourseInfo(dto, module);
        return dto;
    }

    private CourseModuleDto baseModuleDto(CourseModule module) {
        return new CourseModuleDto()
                .setId(module.getId())
                .setTitle(module.getTitle())
                .setDescription(module.getDescription())
                .setOrderIndex(module.getOrderIndex());
    }

    private void applyCourseInfo(CourseModuleDto dto, CourseModule module) {
        Course course = module.getCourse();
        dto.setCourseId(course.getId())
                .setCourseTitle(course.getTitle())
                .setCourseSlug(course.getSlug());
    }

    private LessonDto toLessonDto(Lesson lesson, boolean withResources) {
        CourseModule module = lesson.getModule();
        Course course = module.getCourse();
        LessonDto dto = new LessonDto()
                .setId(lesson.getId())
                .setTitle(lesson.getTitle())
                .setDescription(lesson.getDescription())
                .setLessonType(lesson.getLessonType())
                .setVideoUrl(lesson.getVideoUrl())
                .setVideoEmbedUrl(toEmbedUrl(lesson.getVideoUrl()))
                .setNotes(lesson.getNotes())
                .setDurationMinutes(lesson.getDurationMinutes())
                .setDurationLabel(toDurationLabel(lesson.getDurationMinutes()))
                .setOrderIndex(lesson.getOrderIndex())
                .setPublished(lesson.isPublished())
                .setModuleId(module.getId())
                .setModuleTitle(module.getTitle())
                .setModuleOrderIndex(module.getOrderIndex())
                .setCourseId(course.getId())
                .setCourseSlug(course.getSlug())
                .setCourseTitle(course.getTitle());
        if (withResources) {
            dto.setResources(lesson.getResources().stream()
                    .sorted(Comparator.comparing(LessonResource::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(this::toResourceDto)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private LessonResourceDto toResourceDto(LessonResource resource) {
        return new LessonResourceDto()
                .setId(resource.getId())
                .setTitle(resource.getTitle())
                .setResourceType(resource.getResourceType())
                .setFileUrl(resource.getFileUrl())
                .setStoredFile(isStoredFile(resource.getFileUrl()))
                .setLessonId(resource.getLesson().getId());
    }

    // ================= Internal helpers =================

    private Course requireCourse(Long courseId) {
        return courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found."));
    }

    /**
     * Admin/trainer management entry point. Loads the course and, when the caller is
     * a TRAINER, verifies they own it so they can never manage another trainer's course.
     */
    private Course requireManageableCourse(Long courseId) {
        Course course = requireCourse(courseId);
        assertCanManage(course);
        return course;
    }

    private void assertCanManage(Course course) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails principal)) {
            return;
        }
        if (principal.getUser().getRole() == Role.TRAINER) {
            Long ownerUserId = (course != null && course.getTrainer() != null && course.getTrainer().getUser() != null)
                    ? course.getTrainer().getUser().getId() : null;
            if (ownerUserId == null || !ownerUserId.equals(principal.getUser().getId())) {
                throw new AccessDeniedException("You can only manage courses assigned to you.");
            }
        }
    }

    private CourseModule requireModule(Long moduleId) {
        CourseModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found."));
        assertCanManage(module.getCourse());
        return module;
    }

    private Lesson requireLesson(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found."));
        assertCanManage(lesson.getModule().getCourse());
        return lesson;
    }

    private List<CourseModule> modulesWithLessons(Long courseId) {
        List<CourseModule> modules = moduleRepository.findWithLessonsByCourseId(courseId);
        modules.sort(MODULE_ORDER);
        for (CourseModule module : modules) {
            module.getLessons().sort(LESSON_ORDER);
        }
        return modules;
    }

    private void applyLessonDto(Lesson lesson, LessonDto dto) {
        lesson.setTitle(requireTrimmed(dto.getTitle(), "Lesson title is required."));
        lesson.setDescription(trimToNull(dto.getDescription()));
        lesson.setLessonType(dto.getLessonType());
        lesson.setVideoUrl(trimToNull(dto.getVideoUrl()));
        lesson.setNotes(trimToNull(dto.getNotes()));
        lesson.setDurationMinutes(dto.getDurationMinutes());
        lesson.setPublished(dto.isPublished());
    }

    private void deleteLessonFiles(Lesson lesson) {
        for (LessonResource resource : lesson.getResources()) {
            if (isStoredFile(resource.getFileUrl())) {
                fileStorageService.deleteIfExists(resource.getFileUrl());
            }
        }
    }

    private boolean isStoredFile(String fileUrl) {
        return fileUrl != null && fileUrl.startsWith(FileStorageService.LESSON_RESOURCE_PREFIX);
    }

    private int nextModuleOrder(Long courseId) {
        return moduleRepository.findByCourse_IdOrderByOrderIndexAsc(courseId).stream()
                .map(CourseModule::getOrderIndex)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private int nextLessonOrder(Long moduleId) {
        return lessonRepository.findByModule_IdOrderByOrderIndexAsc(moduleId).stream()
                .map(Lesson::getOrderIndex)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private Lesson requireAccessibleLesson(Long lessonId, User user) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found."));
        if (!lesson.isPublished()) {
            throw new ResourceNotFoundException("Lesson not found.");
        }
        requireEnrollment(lesson.getModule().getCourse().getId(), user);
        return lesson;
    }

    private void requireEnrollment(Long courseId, User user) {
        if (user == null || user.getRole() != Role.STUDENT) {
            throw new BusinessException("Only enrolled students can access course content.");
        }
        Student student = requireStudent(user);
        // SECURITY (Phase 25): content access requires a PAID enrollment. A seat is
        // reserved as PENDING_PAYMENT the moment checkout starts, so a status-agnostic
        // check would let unpaid / failed / cancelled checkouts read paid lessons.
        boolean paid = enrollmentRepository.existsByStudent_IdAndCourse_IdAndStatusIn(
                student.getId(), courseId,
                java.util.List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED));
        if (!paid) {
            throw new BusinessException("You are not enrolled in this course.");
        }
    }

    private Student requireStudent(User user) {
        return studentRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new BusinessException("Student profile is missing. Please contact support."));
    }

    private Set<Long> completedLessonIds(Long courseId, User user) {
        Student student = requireStudent(user);
        return completionRepository.findByStudent_IdAndLesson_Module_Course_Id(student.getId(), courseId).stream()
                .map(completion -> completion.getLesson().getId())
                .collect(Collectors.toSet());
    }

    private String toEmbedUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        try {
            if (lower.startsWith("https://www.youtube.com/watch")
                    || lower.startsWith("http://www.youtube.com/watch")
                    || lower.startsWith("www.youtube.com/watch")) {
                int queryStart = trimmed.indexOf('?');
                if (queryStart >= 0 && queryStart < trimmed.length() - 1) {
                    for (String param : trimmed.substring(queryStart + 1).split("&")) {
                        if (param.startsWith("v=") && param.length() > 2) {
                            return "https://www.youtube.com/embed/" + param.substring(2);
                        }
                    }
                }
            }
            if (lower.startsWith("https://youtu.be/") || lower.startsWith("http://youtu.be/")) {
                String id = trimmed.substring(trimmed.lastIndexOf('/') + 1);
                int query = id.indexOf('?');
                if (query > 0) {
                    id = id.substring(0, query);
                }
                if (!id.isBlank()) {
                    return "https://www.youtube.com/embed/" + id;
                }
            }
        } catch (Exception ignored) {
            // Unsupported video URLs are rendered as external links instead.
        }
        return null;
    }

    private String toDurationLabel(Integer minutes) {
        if (minutes == null || minutes <= 0) {
            return null;
        }
        if (minutes < 60) {
            return minutes + " min";
        }
        long hours = minutes / 60;
        int remainder = minutes % 60;
        return remainder == 0 ? hours + "h" : hours + "h " + remainder + "m";
    }

    private String requireTrimmed(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(errorMessage);
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
