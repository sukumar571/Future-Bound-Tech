package com.futureboundtech.repository;

import com.futureboundtech.entity.Notification;
import com.futureboundtech.enums.NotificationRelatedType;
import com.futureboundtech.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    List<Notification> findTop5ByUser_IdOrderByCreatedAtDesc(Long userId);

    long countByUser_IdAndIsReadFalse(Long userId);

    /** Keeps scheduled reminders to one notification per recipient and event. */
    boolean existsByUser_IdAndTypeAndRelatedTypeAndRelatedId(Long userId, NotificationType type,
                                                            NotificationRelatedType relatedType, Long relatedId);

    List<Notification> findAllByOrderByCreatedAtDesc();
}
