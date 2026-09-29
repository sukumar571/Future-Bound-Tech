package com.futureboundtech.repository;

import com.futureboundtech.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion> findByQuiz_Id(Long quizId);

    long countByQuiz_Id(Long quizId);
}
