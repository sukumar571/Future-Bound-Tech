package com.futureboundtech.service;

import com.futureboundtech.dto.BatchDto;
import com.futureboundtech.dto.CourseDto;
import com.futureboundtech.dto.TrainerDto;
import com.futureboundtech.entity.User;
import com.futureboundtech.enums.CourseCategory;
import com.futureboundtech.enums.CourseLevel;
import com.futureboundtech.enums.CourseStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CourseService {

    List<CourseDto> searchPublic(String query, CourseCategory category, CourseLevel level);

    List<CourseDto> findFeaturedPublic(int limit);

    CourseDto findPublicBySlug(String slug);

    List<CourseDto> searchAdmin(String query, CourseCategory category, CourseLevel level, CourseStatus status);

    CourseDto findById(Long id);

    List<CourseDto> findCoursesForTrainer(User trainerUser);

    CourseDto findAdminById(Long id);

    CourseDto create(CourseDto dto, MultipartFile thumbnail);

    CourseDto update(Long id, CourseDto dto, MultipartFile thumbnail);

    void publish(Long id);

    void unpublish(Long id);

    void deleteSafely(Long id);

    void enrollStudent(String slug, User currentUser);

    void enrollStudent(String slug, User currentUser, Long batchId);

    List<BatchDto> findEnrollableBatches(String slug);

    /** All published batches that are open for enrollment, for the public /batches page. */
    List<BatchDto> findPublicEnrollableBatches();

    boolean isStudentEnrolled(String slug, User currentUser);

    List<TrainerDto> listTrainers();

    long countAll();

    long countPublished();
}
