package com.futureboundtech.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.futureboundtech.enums.CertificateStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A course certificate. The row is only created once the configured eligibility
 * criteria have been satisfied; {@link #certificateNumber} is assigned when the
 * certificate is approved/issued and is what the public verification endpoint
 * resolves. Student and course names are snapshots taken at issue time so the
 * certificate record stays truthful even if profiles change later.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "certificates")
public class Certificate extends BaseEntity {

    /** Unique public ID; only verifiable while the certificate is ISSUED. */
    @NotBlank
    @Column(unique = true, nullable = false)
    private String certificateNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    @JsonIgnore
    private Enrollment enrollment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CertificateStatus status = CertificateStatus.ISSUED;

    /** Snapshot of the holder's full name at issue time. */
    private String studentName;

    /** Snapshot of the course title at issue time. */
    private String courseTitle;

    private LocalDate issueDate;

    /** Free-text reason captured when an admin revokes a certificate. */
    @Column(length = 500)
    private String revokeReason;

    private LocalDateTime revokedAt;

    /** Email of the admin who issued/approved the certificate. */
    private String issuedBy;

    private String certificateUrl;

}
