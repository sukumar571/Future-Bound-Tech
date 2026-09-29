package com.futureboundtech.repository;

import com.futureboundtech.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    @Query("""
            SELECT a FROM Assignment a
            JOIN FETCH a.batch b
            JOIN FETCH b.course
            WHERE b.course.id IN :courseIds
            ORDER BY a.dueDate ASC
            """)
    List<Assignment> findForCourses(@Param("courseIds") List<Long> courseIds);

    List<Assignment> findAllByOrderByCreatedAtDesc();

    List<Assignment> findByBatch_IdOrderByDueDateAsc(Long batchId);

    List<Assignment> findByBatch_IdInOrderByDueDateAsc(java.util.Collection<Long> batchIds);

    /** Published work whose deadline falls inside a reminder window. */
    List<Assignment> findByDueDateBetweenOrderByDueDateAsc(java.time.LocalDateTime from, java.time.LocalDateTime to);
}
