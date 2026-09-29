package com.futureboundtech.repository;

import com.futureboundtech.entity.MockTestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MockTestAttemptRepository extends JpaRepository<MockTestAttempt, Long> {

    // Completed attempts only (submittedAt set).
    List<MockTestAttempt> findByStudent_IdAndSubmittedAtNotNullOrderByCreatedAtDesc(Long studentId);

    List<MockTestAttempt> findByMockTest_IdAndSubmittedAtNotNullOrderByCreatedAtDesc(Long mockTestId);

    long countByMockTest_IdAndStudent_IdAndSubmittedAtNotNull(Long mockTestId, Long studentId);

    Optional<MockTestAttempt> findFirstByMockTest_IdAndStudent_IdAndSubmittedAtIsNull(Long mockTestId, Long studentId);

    Optional<MockTestAttempt> findByIdAndStudent_Id(Long id, Long studentId);
}
