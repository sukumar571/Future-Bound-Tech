package com.futureboundtech.repository;

import com.futureboundtech.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByStudent_IdOrderByCreatedAtDesc(Long studentId);

    Optional<Submission> findByAssignment_IdAndStudent_Id(Long assignmentId, Long studentId);

    boolean existsByAssignment_IdAndStudent_Id(Long assignmentId, Long studentId);

    List<Submission> findByAssignment_IdOrderByCreatedAtDesc(Long assignmentId);

    List<Submission> findByAssignment_IdInOrderByCreatedAtDesc(java.util.Collection<Long> assignmentIds);

    long countByAssignment_IdInAndStatus(java.util.Collection<Long> assignmentIds,
                                         com.futureboundtech.enums.SubmissionStatus status);

    long countByAssignment_Id(Long assignmentId);
}
