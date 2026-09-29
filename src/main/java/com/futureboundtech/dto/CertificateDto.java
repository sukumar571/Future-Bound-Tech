package com.futureboundtech.dto;

import com.futureboundtech.enums.CertificateStatus;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

/** Certificate read model for the admin portal. */
@Data
@Accessors(chain = true)
public class CertificateDto {
    private Long id;
    private String certificateNumber;
    private Long enrollmentId;
    private String studentName;
    private String courseTitle;
    private LocalDate issueDate;
    private String certificateUrl;

    private CertificateStatus status;
    private String statusLabel;
    private String revokeReason;
    private String issuedBy;
    private String verificationUrl;

    public boolean isIssued() {
        return status == CertificateStatus.ISSUED;
    }

    public boolean isPendingApproval() {
        return status == CertificateStatus.PENDING_APPROVAL;
    }

    public boolean isRevoked() {
        return status == CertificateStatus.REVOKED;
    }
}
