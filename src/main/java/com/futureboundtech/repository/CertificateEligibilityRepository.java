package com.futureboundtech.repository;

import com.futureboundtech.entity.CertificateEligibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateEligibilityRepository extends JpaRepository<CertificateEligibility, Long> {

    Optional<CertificateEligibility> findByCourse_Id(Long courseId);

    Optional<CertificateEligibility> findByCourse_IsNull();

    List<CertificateEligibility> findAllByOrderByCourse_IdAsc();
}
