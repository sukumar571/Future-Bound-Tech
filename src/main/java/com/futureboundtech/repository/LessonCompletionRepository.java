package com.futureboundtech.repository;

import com.futureboundtech.entity.LessonCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonCompletionRepository extends JpaRepository<LessonCompletion, Long> {

    Optional<LessonCompletion> findByStudent_IdAndLesson_Id(Long studentId, Long lessonId);

    boolean existsByStudent_IdAndLesson_Id(Long studentId, Long lessonId);

    List<LessonCompletion> findByStudent_IdAndLesson_Module_Course_Id(Long studentId, Long courseId);

    long countByStudent_IdAndLesson_Module_Course_Id(Long studentId, Long courseId);

    long deleteByStudent_IdAndLesson_Id(Long studentId, Long lessonId);
}
