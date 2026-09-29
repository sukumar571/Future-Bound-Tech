package com.futureboundtech.enums;

/**
 * Kind of domain object a notification points at. Stored together with a plain
 * {@code relatedId} so any entity can be referenced without adding a foreign key
 * per type; the UI resolves it to a role-scoped link.
 */
public enum NotificationRelatedType {
    ENROLLMENT,
    PAYMENT,
    LIVE_CLASS,
    ASSIGNMENT,
    QUIZ,
    CERTIFICATE,
    COURSE,
    BATCH,
    ANNOUNCEMENT
}
