package com.futureboundtech.service;

import com.futureboundtech.dto.CertificateDto;
import com.futureboundtech.dto.CertificateEligibilityDto;
import com.futureboundtech.dto.CertificateEvaluationDto;
import com.futureboundtech.dto.CertificateVerifyDto;
import com.futureboundtech.dto.NameValueDto;
import com.futureboundtech.entity.User;

import java.util.List;

/**
 * Certificate lifecycle: eligibility configuration, server-side evaluation,
 * issuing / approving / revoking, PDF generation and public verification.
 *
 * <p>A certificate is never created unless every configured criterion passes
 * the server-side evaluation. With manual approval enabled an eligible record
 * waits in PENDING_APPROVAL until an admin approves it.</p>
 */
public interface CertificateService {

    // ---- Admin: configuration ----------------------------------------------
    List<CertificateEligibilityDto> listConfigs();

    CertificateEligibilityDto getGlobalConfig();

    CertificateEligibilityDto getConfigForCourse(Long courseId);

    void saveConfig(CertificateEligibilityDto dto);

    /** Dropdown options: every non-deleted course (value = course id). */
    List<NameValueDto> courseOptions();

    /** Dropdown options for the final assessment quiz picker. */
    List<NameValueDto> quizOptions();

    // ---- Admin: evaluation / issuing ----------------------------------------
    CertificateEvaluationDto evaluateEnrollment(Long enrollmentId);

    List<CertificateEvaluationDto> evaluationsForCourse(Long courseId);

    /** Issues (or queues for approval) one certificate after full eligibility check. */
    CertificateDto issue(Long enrollmentId, String adminEmail);

    /** Runs eligibility for every certificate-less active/completed enrollment of a course. */
    int[] scanCourse(Long courseId, String adminEmail);

    void approve(Long certificateId, String adminEmail);

    void reject(Long certificateId);

    void revoke(Long certificateId, String reason, String adminEmail);

    // ---- Admin: listing / search --------------------------------------------
    List<CertificateDto> searchCertificates(String query, String statusFilter);

    CertificateDto getCertificate(Long id);

    // ---- PDF -----------------------------------------------------------------
    /** Renders the certificate PDF; students may only download their own issued certificates. */
    byte[] renderCertificatePdf(Long certificateId, User requester, boolean admin);

    String pdfFileName(CertificateDto dto);

    // ---- Public verification ---------------------------------------------------
    CertificateVerifyDto verify(String certificateNumber);

    String instituteName();
}
