package com.futureboundtech.repository;

import com.futureboundtech.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    long countByCourse_Id(Long courseId);

    List<Review> findAllByOrderByCreatedAtDesc();

    List<Review> findByPublishedTrueOrderByCreatedAtDesc();

    long countByIsApprovedTrue();
}
