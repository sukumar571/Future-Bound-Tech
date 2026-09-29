package com.futureboundtech.repository;

import com.futureboundtech.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudent_IdAndCourse_Id(Long studentId, Long courseId);

    boolean existsByStudent_IdAndCourse_IdAndStatusIn(Long studentId, Long courseId,
                                                      java.util.Collection<com.futureboundtech.enums.EnrollmentStatus> statuses);

    Optional<Enrollment> findByStudent_IdAndCourse_Id(Long studentId, Long courseId);

    Optional<Enrollment> findTopByStudent_IdAndCourse_IdOrderByCreatedAtDesc(Long studentId, Long courseId);

    Optional<Enrollment> findByStudent_IdAndCourse_IdAndStatus(Long studentId, Long courseId,
                                                               com.futureboundtech.enums.EnrollmentStatus status);

    long countByCourse_Id(Long courseId);

    long countByStudent_Id(Long studentId);

    long countByStatus(com.futureboundtech.enums.EnrollmentStatus status);

    long countByBatch_Id(Long batchId);

    /** Seats consumed by enrollments that still occupy capacity (pending, active or completed). */
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.batch.id = :batchId "
            + "AND e.status IN (com.futureboundtech.enums.EnrollmentStatus.PENDING_PAYMENT, "
            + "com.futureboundtech.enums.EnrollmentStatus.ACTIVE, "
            + "com.futureboundtech.enums.EnrollmentStatus.COMPLETED)")
    long countSeatsHeldByBatch(@Param("batchId") Long batchId);

    List<Enrollment> findAllByOrderByCreatedAtDesc();

    List<Enrollment> findByStudent_IdOrderByCreatedAtDesc(Long studentId);

    List<Enrollment> findByCourse_Id(Long courseId);

    List<Enrollment> findByBatch_IdOrderByCreatedAtDesc(Long batchId);

    List<Enrollment> findByBatch_IdInOrderByCreatedAtDesc(java.util.Collection<Long> batchIds);

    long countByBatch_IdIn(java.util.Collection<Long> batchIds);

    @Query("""
            SELECT e FROM Enrollment e
            JOIN FETCH e.course c
            LEFT JOIN FETCH c.trainer t
            LEFT JOIN FETCH t.user
            WHERE e.student.id = :studentId
            ORDER BY e.createdAt DESC
            """)
    List<Enrollment> findForStudent(@Param("studentId") Long studentId);
}
