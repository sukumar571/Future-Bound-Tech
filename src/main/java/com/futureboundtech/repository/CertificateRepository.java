package com.futureboundtech.repository;

import com.futureboundtech.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByEnrollment_Id(Long enrollmentId);

    Optional<Certificate> findByCertificateNumber(String certificateNumber);

    Optional<Certificate> findByCertificateNumberIgnoreCase(String certificateNumber);

    boolean existsByCertificateNumber(String certificateNumber);

    long countByCertificateNumberStartingWith(String prefix);

    List<Certificate> findAllByOrderByIssueDateDesc();

    @Query("""
            SELECT c FROM Certificate c
            JOIN FETCH c.enrollment e
            JOIN FETCH e.course
            WHERE e.student.id = :studentId
            ORDER BY c.issueDate DESC
            """)
    List<Certificate> findForStudent(@Param("studentId") Long studentId);
}
