package com.futureboundtech.enums;

import lombok.Getter;

/**
 * What a notification is about. Drives the icon and badge colour in the UI so
 * templates never have to hardcode a mapping.
 *
 * <p>{@link #INFO}, {@link #WARNING}, {@link #ALERT} and {@link #MESSAGE} are the
 * original generic buckets; they are kept so rows written before the notification
 * feature still map correctly.</p>
 */
@Getter
public enum NotificationType {

    ENROLLMENT_CONFIRMATION("Enrollment confirmation", "bi-ui-checks", "text-bg-success"),
    PAYMENT_CONFIRMATION("Payment confirmation", "bi-credit-card", "text-bg-success"),
    CLASS_REMINDER("Class reminder", "bi-camera-video", "text-bg-primary"),
    ASSIGNMENT_DEADLINE("Assignment deadline", "bi-pencil-square", "text-bg-warning"),
    QUIZ_RESULT("Quiz result", "bi-patch-question", "text-bg-info"),
    CERTIFICATE_ISSUED("Certificate issued", "bi-award", "text-bg-success"),
    COURSE_ANNOUNCEMENT("Course announcement", "bi-journal-bookmark", "text-bg-primary"),
    BATCH_ANNOUNCEMENT("Batch announcement", "bi-people", "text-bg-primary"),
    GENERAL_ANNOUNCEMENT("Institute announcement", "bi-megaphone", "text-bg-dark"),

    INFO("Information", "bi-info-circle", "text-bg-secondary"),
    WARNING("Warning", "bi-exclamation-triangle", "text-bg-warning"),
    ALERT("Alert", "bi-exclamation-octagon", "text-bg-danger"),
    MESSAGE("Message", "bi-envelope", "text-bg-primary");

    private final String label;
    private final String icon;
    private final String badgeClass;

    NotificationType(String label, String icon, String badgeClass) {
        this.label = label;
        this.icon = icon;
        this.badgeClass = badgeClass;
    }
}
