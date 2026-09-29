package com.futureboundtech.repository;

import com.futureboundtech.entity.MockTestAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MockTestAnswerRepository extends JpaRepository<MockTestAnswer, Long> {

    @Query("select a from MockTestAnswer a join fetch a.question where a.attempt.id = :attemptId")
    List<MockTestAnswer> findWithQuestionsByAttempt(@Param("attemptId") Long attemptId);

    Optional<MockTestAnswer> findByIdAndAttempt_Id(Long id, Long attemptId);
}
