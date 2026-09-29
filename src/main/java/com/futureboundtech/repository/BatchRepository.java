package com.futureboundtech.repository;

import com.futureboundtech.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {

    long countByCourse_Id(Long courseId);

    List<Batch> findAllByOrderByCreatedAtDesc();

    List<Batch> findByPublishedTrueOrderByStartDateAscIdAsc();

    List<Batch> findByCourse_IdOrderByCreatedAtDesc(Long courseId);

    List<Batch> findByTrainer_IdOrderByCreatedAtDesc(Long trainerId);
}
