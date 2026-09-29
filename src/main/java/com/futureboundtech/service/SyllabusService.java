package com.futureboundtech.service;

import com.futureboundtech.dto.CourseModuleDto;
import com.futureboundtech.dto.CourseProgressDto;
import com.futureboundtech.dto.LessonDto;
import com.futureboundtech.dto.LessonResourceDto;
import com.futureboundtech.entity.LessonResource;
import com.futureboundtech.entity.User;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SyllabusService {

    // ================= Admin / Trainer management =================

    List<CourseModuleDto> getAdminSyllabus(Long courseId);

    CourseModuleDto getModuleForAdmin(Long moduleId);

    LessonDto getLessonForEdit(Long lessonId);

    CourseModuleDto createModule(Long courseId, CourseModuleDto dto);

    CourseModuleDto updateModule(Long moduleId, CourseModuleDto dto);

    Long deleteModule(Long moduleId);

    Long moveModule(Long moduleId, int direction);

    LessonDto createLesson(Long moduleId, LessonDto dto);

    LessonDto updateLesson(Long lessonId, LessonDto dto);

    Long deleteLesson(Long lessonId);

    Long moveLesson(Long lessonId, int direction);

    LessonDto setLessonPublished(Long lessonId, boolean published);

    LessonResourceDto addResource(Long lessonId, LessonResourceDto dto, MultipartFile file);

    Long deleteResource(Long resourceId);

    List<String> getResourceTypes();

    // ================= Student learning =================

    List<CourseModuleDto> getStudentSyllabus(Long courseId, User user);

    LessonDto getStudentLesson(Long lessonId, User user);

    void setLessonCompleted(Long lessonId, User user, boolean completed);

    CourseProgressDto getCourseProgress(Long courseId, User user);

    // ================= Public catalog =================

    List<CourseModuleDto> getPublicPreview(Long courseId);

    // ================= Resource download =================

    LessonResource authorizeResource(Long lessonId, Long resourceId, User user);

    /** Authorizes an ADMIN or the owning TRAINER to preview a stored lesson resource. */
    LessonResource authorizeResourcePreview(Long lessonId, Long resourceId, User user);

    Resource loadStoredResourceFile(LessonResource resource);
}
