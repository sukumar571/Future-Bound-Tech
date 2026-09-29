package com.futureboundtech.enums;

/**
 * Lifecycle of a course enrollment.
 *
 * <p>An enrollment only becomes {@link #ACTIVE} after the institute has received a
 * server-verified payment (or an admin grants a free seat). It is never activated
 * purely from a frontend signal.</p>
 */
public enum EnrollmentStatus {
    PENDING_PAYMENT,
    ACTIVE,
    COMPLETED,
    CANCELLED,
    EXPIRED
}
