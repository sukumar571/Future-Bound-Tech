package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

/** Public verification result — deliberately minimal, never exposes emails or payments. */
@Data
@Accessors(chain = true)
public class CertificateVerifyDto {

    private String queriedNumber;

    /** True only when the certificate exists and is currently ISSUED. */
    private boolean valid;

    /** NOT_FOUND / VALID / REVOKED / PENDING_APPROVAL */
    private String outcome;

    private String studentName;
    private String courseTitle;
    private LocalDate issueDate;
    private String institutionName;
    private String revokeReason;
}
