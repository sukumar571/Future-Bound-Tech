package com.futureboundtech.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Admin-configurable rules that decide when a student earns a certificate.
 *
 * <p>A row with a {@code null} course is the institute-wide default; a row bound
 * to a course overrides the default for that course only. A threshold of 0 for
 * assignments/quizzes or a null final-assessment quiz means "not required".
 * Certificates are never issued automatically unless every configured criterion
 * is met server-side.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "certificate_eligibility")
public class CertificateEligibility extends BaseEntity {

    /** Null row = institute-wide default configuration. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", unique = true)
    private Course course;

    @Min(0)
    @Max(100)
    @Column(nullable = false)
    @Builder.Default
    private Integer minCourseCompletionPercent = 100;

    /** Minimum % of published assignments submitted. 0 = not required. */
    @Min(0)
    @Max(100)
    @Column(nullable = false)
    @Builder.Default
    private Integer minAssignmentCompletionPercent = 0;

    /** Minimum average of best quiz scores across the course's published quizzes. 0 = not required. */
    @Min(0)
    @Max(100)
    @Column(nullable = false)
    @Builder.Default
    private Integer minQuizAveragePercent = 0;

    /** Quiz designated as the course's final assessment. Null = not required. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_assessment_quiz_id")
    private Quiz finalAssessmentQuiz;

    /** Minimum best-score % required on the final assessment quiz. */
    @Min(0)
    @Max(100)
    @Column(nullable = false)
    @Builder.Default
    private Integer minFinalAssessmentPercent = 0;

    /** When true an admin must approve each eligible certificate before it is issued. */
    @Column(name = "manual_approval_required")
    @Builder.Default
    private boolean manualApprovalRequired = false;

    // ---- Appearance (read from the global default row) ---------------------

    @Builder.Default
    @Column(nullable = false, length = 120)
    private String certificateTitle = "Certificate of Completion";

    /** Name printed under the authorised-signature line on the PDF. */
    private String signatoryName;

    /** Role printed under the signatory name, e.g. "Course Director". */
    private String signatoryDesignation;
}
