package com.futureboundtech.repository;

import com.futureboundtech.entity.ContactMessage;
import com.futureboundtech.enums.ContactStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    List<ContactMessage> findAllByOrderByCreatedAtDesc();

    long countByIsRepliedFalse();

    long countByStatus(ContactStatus status);

    /** Free-text search over name/email/message with an optional status filter; nulls mean "any". */
    @Query("SELECT c FROM ContactMessage c "
            + "WHERE (:q IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "   OR LOWER(c.email) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "   OR LOWER(c.message) LIKE LOWER(CONCAT('%', :q, '%'))) "
            + "AND (:status IS NULL OR c.status = :status) "
            + "ORDER BY c.createdAt DESC")
    List<ContactMessage> search(@Param("q") String q, @Param("status") ContactStatus status);
}
