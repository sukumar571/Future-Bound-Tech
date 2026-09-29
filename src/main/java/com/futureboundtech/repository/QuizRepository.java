package com.futureboundtech.repository;

import com.futureboundtech.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    long countByCourse_Id(Long courseId);

    List<Quiz> findAllByOrderByCreatedAtDesc();

    @Query("""
            SELECT q FROM Quiz q
            JOIN FETCH q.course
            WHERE q.course.id IN :courseIds
            ORDER BY q.title ASC
            """)
    List<Quiz> findForCourses(@Param("courseIds") List<Long> courseIds);

    @Query("""
            SELECT q FROM Quiz q
            JOIN FETCH q.course
            WHERE q.id = :quizId
            """)
    Optional<Quiz> findWithCourse(@Param("quizId") Long quizId);
}
