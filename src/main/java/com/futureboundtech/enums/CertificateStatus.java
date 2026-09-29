package com.futureboundtech.enums;

/**
 * Lifecycle of a course certificate.
 *
 * <p>A certificate is only ever created after the configured eligibility criteria
 * have been verified server-side. When manual approval is required the record
 * waits in {@link #PENDING_APPROVAL} until an admin approves it; only then does
 * it receive a certificate number and become downloadable.</p>
 */
public enum CertificateStatus {
    PENDING_APPROVAL,
    ISSUED,
    REVOKED
}
