package com.futureboundtech.repository;

import com.futureboundtech.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findByBatchIsNullOrderByCreatedAtDesc();

    List<Announcement> findAllByOrderByCreatedAtDesc();

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.batch IS NULL
               OR a.batch.course.id IN :courseIds
            ORDER BY a.createdAt DESC
            """)
    List<Announcement> findForStudent(@Param("courseIds") List<Long> courseIds);

    List<Announcement> findByBatch_IdInOrderByCreatedAtDesc(java.util.Collection<Long> batchIds);
}
