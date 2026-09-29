package com.futureboundtech.repository;

import com.futureboundtech.entity.CodingSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CodingSubmissionRepository extends JpaRepository<CodingSubmission, Long> {

    Optional<CodingSubmission> findByStudent_IdAndQuestion_Id(Long studentId, Long questionId);
}
