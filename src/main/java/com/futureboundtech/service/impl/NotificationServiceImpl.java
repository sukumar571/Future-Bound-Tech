package com.futureboundtech.service.impl;

import com.futureboundtech.dto.NotificationDto;
import com.futureboundtech.dto.NotificationSendDto;
import com.futureboundtech.entity.Announcement;
import com.futureboundtech.entity.Batch;
import com.futureboundtech.entity.Certificate;
import com.futureboundtech.entity.Enrollment;
import com.futureboundtech.entity.Notification;
import com.futureboundtech.entity.Payment;
import com.futureboundtech.entity.Student;
import com.futureboundtech.entity.Trainer;
import com.futureboundtech.entity.User;
import com.futureboundtech.enums.NotificationAudience;
import com.futureboundtech.enums.NotificationRelatedType;
import com.futureboundtech.enums.NotificationType;
import com.futureboundtech.enums.Role;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.exception.ResourceNotFoundException;
import com.futureboundtech.repository.CourseRepository;
import com.futureboundtech.repository.EnrollmentRepository;
import com.futureboundtech.repository.NotificationRepository;
import com.futureboundtech.repository.StudentRepository;
import com.futureboundtech.repository.TrainerRepository;
import com.futureboundtech.repository.UserRepository;
import com.futureboundtech.service.EmailNotificationService;
import com.futureboundtech.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default notification backend: one persisted inbox row per recipient, plus an
 * optional e-mail copy when SMTP has been configured.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    /** Feed length shown on the admin oversight screen. */
    private static final int ADMIN_FEED_LIMIT = 200;
    private static final int TITLE_MAX = 160;
    private static final int MESSAGE_MAX = 1000;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TrainerRepository trainerRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final EmailNotificationService emailNotificationService;

    // ================= Creation =================

    @Override
    @Transactional
    public NotificationDto notify(User recipient, NotificationType type, String title, String message,
                                  NotificationRelatedType relatedType, Long relatedId) {
        if (recipient == null || type == null || !StringUtils.hasText(message)) {
            return null;
        }
        Notification saved = notificationRepository.save(build(recipient, type, title, message, relatedType, relatedId));
        dispatchEmail(saved);
        return toDto(saved, recipient.getRole());
    }

    @Override
    @Transactional
    public int notifyAll(Collection<User> recipients, NotificationType type, String title, String message,
                         NotificationRelatedType relatedType, Long relatedId) {
        List<User> unique = distinctStudents(recipients);
        if (unique.isEmpty() || type == null || !StringUtils.hasText(message)) {
            return 0;
        }
        List<Notification> rows = new ArrayList<>(unique.size());
        for (User recipient : unique) {
            rows.add(build(recipient, type, title, message, relatedType, relatedId));
        }
        notificationRepository.saveAll(rows);
        if (emailNotificationService.isEnabled()) {
            rows.forEach(this::dispatchEmail);
        }
        return rows.size();
    }

    @Override
    @Transactional
    public boolean notifyOnce(User recipient, NotificationType type, String title, String message,
                              NotificationRelatedType relatedType, Long relatedId) {
        if (recipient == null || type == null || relatedType == null || relatedId == null) {
            return false;
        }
        if (notificationRepository.existsByUser_IdAndTypeAndRelatedTypeAndRelatedId(
                recipient.getId(), type, relatedType, relatedId)) {
            return false;
        }
        notify(recipient, type, title, message, relatedType, relatedId);
        return true;
    }

    // ================= System triggers =================

    @Override
    @Transactional
    public void enrollmentConfirmed(Enrollment enrollment) {
        if (enrollment == null || enrollment.getStudent() == null || enrollment.getStudent().getUser() == null) {
            return;
        }
        User recipient = enrollment.getStudent().getUser();
        String course = courseTitle(enrollment);
        String batch = batchName(enrollment);
        String title = "Enrollment confirmed";
        String message = "You are enrolled in " + course
                + (batch == null ? "" : " (" + batch + ")")
                + ". Your classes are open now — head to My Courses to start learning.";
        notify(recipient, NotificationType.ENROLLMENT_CONFIRMATION, title, message,
                NotificationRelatedType.ENROLLMENT, enrollment.getId());
    }

    @Override
    @Transactional
    public void paymentConfirmed(Payment payment) {
        if (payment == null || payment.getStudent() == null || payment.getStudent().getUser() == null) {
            return;
        }
        User recipient = payment.getStudent().getUser();
        String title = "Payment received";
        String message = "We received " + amount(payment.getAmount()) + " for " + courseTitle(payment)
                + ". Receipt " + payment.getReceiptNumber() + " is available in your payment history.";
        notify(recipient, NotificationType.PAYMENT_CONFIRMATION, title, message,
                NotificationRelatedType.PAYMENT, payment.getId());
    }

    @Override
    @Transactional
    public void quizResult(User student, String quizTitle, int scorePercent, boolean passed,
                           int correct, int total, Long quizId) {
        String title = (passed ? "Passed: " : "Result: ") + quizTitle;
        String message = "You scored " + scorePercent + "% on \"" + quizTitle + "\" ("
                + correct + " of " + total + " correct). "
                + (passed ? "Nice work — this quiz is cleared." : "Review the lesson and try again when you are ready.");
        notify(student, NotificationType.QUIZ_RESULT, title, message, NotificationRelatedType.QUIZ, quizId);
    }

    @Override
    @Transactional
    public void certificateIssued(Certificate certificate) {
        if (certificate == null || certificate.getEnrollment() == null
                || certificate.getEnrollment().getStudent() == null
                || certificate.getEnrollment().getStudent().getUser() == null) {
            return;
        }
        User recipient = certificate.getEnrollment().getStudent().getUser();
        String title = "Your certificate is ready";
        String message = "Your certificate for " + safe(certificate.getCourseTitle(), "your course")
                + " has been issued. Certificate number " + certificate.getCertificateNumber()
                + " — download it from the Certificates page.";
        notify(recipient, NotificationType.CERTIFICATE_ISSUED, title, message,
                NotificationRelatedType.CERTIFICATE, certificate.getId());
    }

    @Override
    @Transactional
    public void announcementPublished(Announcement announcement) {
        if (announcement == null) {
            return;
        }
        Batch batch = announcement.getBatch();
        if (batch == null) {
            notifyAll(activeStudents(), NotificationType.GENERAL_ANNOUNCEMENT,
                    announcement.getTitle(), announcement.getContent(),
                    NotificationRelatedType.ANNOUNCEMENT, announcement.getId());
            return;
        }
        notifyAll(studentsOfBatch(batch.getId()), NotificationType.BATCH_ANNOUNCEMENT,
                announcement.getTitle(), announcement.getContent(),
                NotificationRelatedType.ANNOUNCEMENT, announcement.getId());
    }

    // ================= Manual send =================

    @Override
    @Transactional
    public int send(NotificationSendDto form) {
        if (form == null) {
            throw new BusinessException("Nothing to send.");
        }
        NotificationType type = form.getType() == null ? NotificationType.GENERAL_ANNOUNCEMENT : form.getType();
        NotificationAudience audience = form.getAudience() == null
                ? NotificationAudience.ALL_STUDENTS : form.getAudience();

        List<User> recipients;
        NotificationRelatedType relatedType = null;
        Long relatedId = null;

        switch (audience) {
            case ALL_STUDENTS -> recipients = activeStudents();
            case COURSE -> {
                requireId(form.getCourseId(), "Choose the course to notify.");
                courseRepository.findById(form.getCourseId())
                        .orElseThrow(() -> new ResourceNotFoundException("Course not found."));
                recipients = studentsOfCourse(form.getCourseId());
                relatedType = NotificationRelatedType.COURSE;
                relatedId = form.getCourseId();
            }
            case BATCH -> {
                requireId(form.getBatchId(), "Choose the batch to notify.");
                recipients = studentsOfBatch(form.getBatchId());
                relatedType = NotificationRelatedType.BATCH;
                relatedId = form.getBatchId();
            }
            case STUDENT -> {
                requireId(form.getStudentId(), "Choose the student to notify.");
                Student student = studentRepository.findById(form.getStudentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
                recipients = List.of(student.getUser());
                relatedType = null;
                relatedId = null;
            }
            case ALL_TRAINERS -> recipients = activeTrainers();
            case TRAINER -> {
                requireId(form.getTrainerId(), "Choose the trainer to notify.");
                Trainer trainer = trainerRepository.findById(form.getTrainerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Trainer not found."));
                if (trainer.getUser() == null) {
                    throw new BusinessException("That trainer has no account to notify.");
                }
                recipients = List.of(trainer.getUser());
                relatedType = null;
                relatedId = null;
            }
            default -> throw new BusinessException("Unsupported audience.");
        }

        if (recipients.isEmpty()) {
            throw new BusinessException(audience == NotificationAudience.ALL_TRAINERS
                    || audience == NotificationAudience.TRAINER
                    ? "That audience has no trainers to notify yet."
                    : "That audience has no students to notify yet.");
        }
        return notifyAll(recipients, type, form.getTitle(), form.getMessage(), relatedType, relatedId);
    }

    @Override
    @Transactional
    public int courseAnnouncement(Long courseId, String title, String message) {
        requireId(courseId, "Choose the course to announce to.");
        return notifyAll(studentsOfCourse(courseId), NotificationType.COURSE_ANNOUNCEMENT,
                title, message, NotificationRelatedType.COURSE, courseId);
    }

    // ================= Inbox =================

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> inbox(User user) {
        return inbox(user, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> inbox(User user, boolean unreadOnly) {
        if (user == null) {
            return List.of();
        }
        List<com.futureboundtech.entity.Notification> rows = unreadOnly
                ? notificationRepository.findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(user.getId())
                : notificationRepository.findByUser_IdOrderByCreatedAtDesc(user.getId());
        return rows.stream().map(n -> toDto(n, user.getRole())).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> recent(User user, int limit) {
        if (user == null) {
            return List.of();
        }
        return notificationRepository.findTop5ByUser_IdOrderByCreatedAtDesc(user.getId()).stream()
                .limit(Math.max(limit, 0))
                .map(n -> toDto(n, user.getRole()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(User user) {
        return user == null ? 0L : notificationRepository.countByUser_IdAndIsReadFalse(user.getId());
    }

    @Override
    @Transactional
    public void markRead(User user, Long notificationId) {
        owned(user, notificationId).ifPresent(row -> {
            if (!row.isRead()) {
                row.setRead(true);
                notificationRepository.save(row);
            }
        });
    }

    @Override
    @Transactional
    public void markAllRead(User user) {
        if (user == null) {
            return;
        }
        List<Notification> unread = notificationRepository.findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(user.getId());
        unread.forEach(row -> row.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Override
    @Transactional
    public void delete(User user, Long notificationId) {
        // Deleting is the dismissal path: only the owner's row is ever touched.
        owned(user, notificationId).ifPresent(notificationRepository::delete);
    }

    // ================= Admin oversight =================

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> listAll() {
        return notificationRepository
                .findAll(PageRequest.of(0, ADMIN_FEED_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent().stream()
                .map(this::toAdminDto)
                .collect(Collectors.toList());
    }

    // ================= Helpers =================

    private Notification build(User recipient, NotificationType type, String title,
                              String message, NotificationRelatedType relatedType,
                              Long relatedId) {
        return Notification.builder()
                .user(recipient)
                .title(truncate(sanitize(title), TITLE_MAX))
                .message(truncate(sanitize(message), MESSAGE_MAX))
                .type(type)
                .relatedType(relatedType)
                .relatedId(relatedId)
                .build();
    }

    /** A notification is only visible to its recipient; anything else is a 404-equivalent. */
    private Optional<Notification> owned(User user, Long notificationId) {
        if (user == null || notificationId == null) {
            return Optional.empty();
        }
        return notificationRepository.findById(notificationId)
                .filter(n -> n.getUser() != null && n.getUser().getId().equals(user.getId()));
    }

    private List<User> activeStudents() {
        return userRepository.findByRoleOrderByCreatedAtDesc(Role.STUDENT).stream()
                .filter(u -> u.isActive())
                .collect(Collectors.toList());
    }

    private List<User> activeTrainers() {
        return userRepository.findByRoleOrderByCreatedAtDesc(Role.TRAINER).stream()
                .filter(u -> u.isActive())
                .collect(Collectors.toList());
    }

    private List<User> studentsOfCourse(Long courseId) {
        return usersFrom(enrollmentRepository.findByCourse_Id(courseId));
    }

    private List<User> studentsOfBatch(Long batchId) {
        return usersFrom(enrollmentRepository.findByBatch_IdOrderByCreatedAtDesc(batchId));
    }

    private List<User> usersFrom(List<Enrollment> enrollments) {
        return enrollments.stream()
                .map(e -> e.getStudent() == null ? null : e.getStudent().getUser())
                .filter(Objects::nonNull)
                .filter(User::isActive)
                .collect(Collectors.toList());
    }

    private List<User> distinctStudents(Collection<User> recipients) {
        Set<User> seen = new LinkedHashSet<>();
        for (User user : recipients) {
            if (user != null && user.isActive()) {
                seen.add(user);
            }
        }
        return new ArrayList<>(seen);
    }

    private void dispatchEmail(Notification row) {
        if (!emailNotificationService.isEnabled()) {
            return;
        }
        User recipient = row.getUser();
        if (recipient == null) {
            return;
        }
        String subject = StringUtils.hasText(row.getTitle()) ? row.getTitle() : row.getType().getLabel();
        emailNotificationService.send(recipient.getEmail(), subject, row.getMessage());
    }

    private NotificationDto toDto(Notification n, Role role) {
        NotificationType type = n.getType() == null ? NotificationType.INFO : n.getType();
        NotificationDto dto = new NotificationDto()
                .setId(n.getId())
                .setTitle(n.getTitle())
                .setMessage(n.getMessage())
                .setType(type)
                .setTypeLabel(type.getLabel())
                .setIcon(type.getIcon())
                .setBadgeClass(type.getBadgeClass())
                .setRelatedType(n.getRelatedType())
                .setRelatedId(n.getRelatedId())
                .setRead(n.isRead())
                .setCreatedAt(n.getCreatedAt());
        String[] action = actionFor(role, n.getRelatedType());
        if (action != null) {
            dto.setActionUrl(action[0]).setActionLabel(action[1]);
        }
        return dto;
    }

    private NotificationDto toAdminDto(Notification n) {
        User recipient = n.getUser();
        NotificationDto dto = toDto(n, recipient == null ? Role.STUDENT : recipient.getRole());
        if (recipient != null) {
            dto.setRecipientName(fullName(recipient))
                    .setRecipientEmail(recipient.getEmail());
        }
        return dto;
    }

    /** Related-entity pointers resolve to the list page that can display them, per role. */
    private String[] actionFor(Role role, NotificationRelatedType relatedType) {
        if (relatedType == null || role == null) {
            return null;
        }
        if (role == Role.STUDENT) {
            return switch (relatedType) {
                case ENROLLMENT, COURSE -> new String[]{"/student/courses", "Open my courses"};
                case PAYMENT -> new String[]{"/student/payments", "View payments"};
                case LIVE_CLASS, BATCH -> new String[]{"/student/classes", "View my classes"};
                case ASSIGNMENT -> new String[]{"/student/assignments", "View assignments"};
                case QUIZ -> new String[]{"/student/quizzes", "View quizzes"};
                case CERTIFICATE -> new String[]{"/student/certificates", "View certificates"};
                case ANNOUNCEMENT -> new String[]{"/student/announcements", "Read announcements"};
            };
        }
        if (role == Role.TRAINER) {
            return switch (relatedType) {
                case ANNOUNCEMENT -> new String[]{"/trainer/announcements", "View announcements"};
                case ASSIGNMENT -> new String[]{"/trainer/assignments", "View assignments"};
                case LIVE_CLASS, BATCH -> new String[]{"/trainer/classes", "View classes"};
                case QUIZ -> new String[]{"/trainer/quizzes", "View quizzes"};
                default -> null;
            };
        }
        if (role == Role.ADMIN) {
            return switch (relatedType) {
                case ANNOUNCEMENT -> new String[]{"/admin/announcements", "Manage announcements"};
                case ENROLLMENT, COURSE -> new String[]{"/admin/enrollments", "View enrollments"};
                case PAYMENT -> new String[]{"/admin/payments", "View payments"};
                case CERTIFICATE -> new String[]{"/admin/certificates", "View certificates"};
                case BATCH -> new String[]{"/admin/batches", "View batches"};
                default -> null;
            };
        }
        return null;
    }

    private void requireId(Long id, String message) {
        if (id == null) {
            throw new BusinessException(message);
        }
    }

    private String courseTitle(Enrollment enrollment) {
        return enrollment.getCourse() == null ? "your course" : safe(enrollment.getCourse().getTitle(), "your course");
    }

    private String courseTitle(Payment payment) {
        return payment.getCourse() == null ? "your course" : safe(payment.getCourse().getTitle(), "your course");
    }

    private String batchName(Enrollment enrollment) {
        return enrollment.getBatch() == null ? null : safe(enrollment.getBatch().getBatchName(), null);
    }

    private String amount(java.math.BigDecimal value) {
        if (value == null) {
            return "the payment";
        }
        return "₹" + value.stripTrailingZeros().toPlainString();
    }

    private String fullName(User user) {
        return safe(user.getFirstName(), "") + " " + safe(user.getLastName(), "");
    }

    private String safe(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String sanitize(String value) {
        return value == null ? null : value.replace("\r\n", "\n").trim();
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    /** Exposed for the reminder scheduler's message formatting. */
    static String formatDateTime(LocalDateTime value) {
        return value == null ? "shortly" : value.format(DATE_TIME);
    }
}
