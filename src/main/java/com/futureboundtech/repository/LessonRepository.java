package com.futureboundtech.repository;

import com.futureboundtech.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByModule_IdOrderByOrderIndexAsc(Long moduleId);

    List<Lesson> findByModule_Id(Long moduleId);

    long countByModule_Course_Id(Long courseId);

    long countByModule_Course_IdAndPublishedTrue(Long courseId);

    List<Lesson> findByModule_Course_IdAndPublishedTrueOrderByModule_OrderIndexAscOrderIndexAsc(Long courseId);

    List<Lesson> findByModule_Course_Id(Long courseId);
}
