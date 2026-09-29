package com.futureboundtech.repository;

import com.futureboundtech.entity.PracticeAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PracticeAttemptRepository extends JpaRepository<PracticeAttempt, Long> {

    List<PracticeAttempt> findByStudent_IdOrderByCreatedAtDesc(Long studentId);

    long countByStudent_Id(Long studentId);

    long countByStudent_IdAndCorrectTrue(Long studentId);
}
