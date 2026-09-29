package com.futureboundtech.repository;

import com.futureboundtech.entity.PracticeQuestion;
import com.futureboundtech.enums.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PracticeQuestionRepository extends JpaRepository<PracticeQuestion, Long> {

    List<PracticeQuestion> findAllByOrderByCreatedAtDesc();

    List<PracticeQuestion> findByPublishedTrueOrderByCategoryAscCreatedAtDesc();

    List<PracticeQuestion> findByTypeAndPublishedTrueOrderByCreatedAtDesc(QuestionType type);

    long countByPublishedTrue();

    long countByTypeAndPublishedTrue(com.futureboundtech.enums.QuestionType type);

    @Query("select distinct q.topic from PracticeQuestion q where q.topic is not null order by q.topic asc")
    List<String> findDistinctTopics();
}
