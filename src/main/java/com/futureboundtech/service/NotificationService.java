package com.futureboundtech.service;

import com.futureboundtech.dto.NotificationDto;
import com.futureboundtech.dto.NotificationSendDto;
import com.futureboundtech.entity.Announcement;
import com.futureboundtech.entity.Certificate;
import com.futureboundtech.entity.Enrollment;
import com.futureboundtech.entity.Payment;
import com.futureboundtech.entity.User;
import com.futureboundtech.enums.NotificationRelatedType;
import com.futureboundtech.enums.NotificationType;

import java.util.Collection;
import java.util.List;

/**
 * Owns the notification inbox: creating rows, fanning one message out to an
 * audience, and every read/update a user can perform on their own inbox.
 *
 * <p>Notifications are always per-recipient rows, which is what allows the read
 * flag and dismiss/delete to be private to each student.</p>
 */
public interface NotificationService {

    // ================= Creation =================

    /** Delivers one notification to one recipient. Returns the persisted row. */
    NotificationDto notify(User recipient, NotificationType type, String title, String message,
                           NotificationRelatedType relatedType, Long relatedId);

    /** Delivers the same notification to many recipients; returns how many were created. */
    int notifyAll(Collection<User> recipients, NotificationType type, String title, String message,
                  NotificationRelatedType relatedType, Long relatedId);

    /**
     * Delivers only when this recipient has never been told about this exact event.
     * Used by the reminder scheduler so an hourly sweep cannot spam a inbox.
     */
    boolean notifyOnce(User recipient, NotificationType type, String title, String message,
                       NotificationRelatedType relatedType, Long relatedId);

    // ================= System triggers =================

    /** "You are enrolled" — fired when an enrollment becomes ACTIVE. */
    void enrollmentConfirmed(Enrollment enrollment);

    /** "Payment received" — fired when a payment settles. */
    void paymentConfirmed(Payment payment);

    /** "Quiz result" — fired when a student submits a quiz. */
    void quizResult(User student, String quizTitle, int scorePercent, boolean passed,
                    int correct, int total, Long quizId);

    /** "Certificate issued" — fired when a certificate reaches ISSUED. */
    void certificateIssued(Certificate certificate);

    /**
     * Turns a published announcement into notifications for its audience:
     * institute-wide for a global post, batch students for a batch post.
     */
    void announcementPublished(Announcement announcement);

    // ================= Manual send =================

    /**
     * Admin/trainer broadcast resolved from the form's audience. Returns the number
     * of recipients reached; throws a business exception on an invalid audience pairing.
     */
    int send(NotificationSendDto form);

    /** Notifications produced from a course-wide announcement. */
    int courseAnnouncement(Long courseId, String title, String message);

    // ================= Inbox =================

    List<NotificationDto> inbox(User user);

    List<NotificationDto> inbox(User user, boolean unreadOnly);

    List<NotificationDto> recent(User user, int limit);

    long unreadCount(User user);

    /** Marks read; silently ignores an id the user does not own. */
    void markRead(User user, Long notificationId);

    void markAllRead(User user);

    /** Dismiss/delete. Ownership is enforced server-side. */
    void delete(User user, Long notificationId);

    // ================= Admin oversight =================

    /** Newest-first feed of every notification, for the admin console. */
    List<NotificationDto> listAll();
}
