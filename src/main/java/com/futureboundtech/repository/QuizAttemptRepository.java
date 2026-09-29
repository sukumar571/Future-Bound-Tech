package com.futureboundtech.repository;

import com.futureboundtech.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByStudent_Id(Long studentId);

    List<QuizAttempt> findByQuiz_IdAndStudent_Id(Long quizId, Long studentId);

    Optional<QuizAttempt> findTopByQuiz_IdAndStudent_IdOrderByCreatedAtDesc(Long quizId, Long studentId);

    long countByQuiz_IdAndStudent_Id(Long quizId, Long studentId);

    long countByQuiz_Id(Long quizId);

    List<QuizAttempt> findByQuiz_IdOrderByCreatedAtDesc(Long quizId);

    List<QuizAttempt> findByStudent_IdOrderByCreatedAtDesc(Long studentId);
}
