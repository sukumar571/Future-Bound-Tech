package com.futureboundtech.repository;

import com.futureboundtech.entity.MockTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MockTestRepository extends JpaRepository<MockTest, Long> {

    List<MockTest> findAllByOrderByCreatedAtDesc();

    List<MockTest> findByPublishedTrueOrderByCreatedAtDesc();

    long countByPublishedTrue();

    @Query("select mt from MockTest mt left join fetch mt.questions where mt.id = :id")
    Optional<MockTest> findWithQuestions(@Param("id") Long id);
}
