package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/** Admin form / read model for certificate eligibility rules. */
@Data
@Accessors(chain = true)
public class CertificateEligibilityDto {

    private Long id;

    /** Null course id = institute-wide default configuration. */
    private Long courseId;
    private String courseTitle;

    private Integer minCourseCompletionPercent;
    private Integer minAssignmentCompletionPercent;
    private Integer minQuizAveragePercent;

    /** Quiz designated as the final assessment; null = not required. */
    private Long finalAssessmentQuizId;
    private Integer minFinalAssessmentPercent;

    private boolean manualApprovalRequired;

    // ---- Appearance (stored on the global default row) ---------------------
    private String certificateTitle;
    private String signatoryName;
    private String signatoryDesignation;
}
