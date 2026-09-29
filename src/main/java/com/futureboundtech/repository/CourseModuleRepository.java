package com.futureboundtech.repository;

import com.futureboundtech.entity.CourseModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseModuleRepository extends JpaRepository<CourseModule, Long> {

    @Query("""
            SELECT DISTINCT m FROM CourseModule m
            LEFT JOIN FETCH m.lessons
            LEFT JOIN FETCH m.course
            WHERE m.course.id = :courseId
            """)
    List<CourseModule> findWithLessonsByCourseId(@Param("courseId") Long courseId);

    List<CourseModule> findByCourse_IdOrderByOrderIndexAsc(Long courseId);

    long countByCourse_Id(Long courseId);
}
