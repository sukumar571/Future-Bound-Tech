package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Data
@Accessors(chain = true)
public class StudentCertificateDto {

    private Long id;
    private String certificateNumber;
    private Long courseId;
    private String courseTitle;
    private LocalDate issueDate;
    private String certificateUrl;

    private String statusLabel;
    private boolean issued;
    private boolean pendingApproval;
    private boolean revoked;
    private String verificationUrl;
}
