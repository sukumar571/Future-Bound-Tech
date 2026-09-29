package com.futureboundtech.repository;

import com.futureboundtech.entity.MentorMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MentorMessageRepository extends JpaRepository<MentorMessage, Long> {

    /** Full history for one student, oldest first — used to render the chat page. */
    List<MentorMessage> findByStudent_IdOrderByCreatedAtAsc(Long studentId);

    /** Most recent turns first — the service trims this to a bounded replay window. */
    List<MentorMessage> findByStudent_IdOrderByCreatedAtDesc(Long studentId);

    void deleteByStudent_Id(Long studentId);
}
