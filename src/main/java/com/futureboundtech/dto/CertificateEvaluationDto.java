package com.futureboundtech.dto;

import com.futureboundtech.enums.CertificateStatus;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/** Result of evaluating one enrollment against the configured certificate rules. */
@Data
@Accessors(chain = true)
public class CertificateEvaluationDto {

    private Long enrollmentId;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private Long courseId;
    private String courseTitle;
    private String enrollmentStatusLabel;

    private boolean eligible;
    private List<CertificateCriterionDto> criteria = new ArrayList<>();

    /** Existing certificate for this enrollment, if any. */
    private boolean hasCertificate;
    private Long certificateId;
    private String certificateNumber;
    private CertificateStatus certificateStatus;
    private String certificateStatusLabel;
}
