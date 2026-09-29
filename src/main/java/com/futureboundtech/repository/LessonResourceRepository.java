package com.futureboundtech.repository;

import com.futureboundtech.entity.LessonResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonResourceRepository extends JpaRepository<LessonResource, Long> {

    List<LessonResource> findByLesson_Id(Long lessonId);
}
