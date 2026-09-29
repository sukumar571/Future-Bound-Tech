package com.futureboundtech.controller;

import com.futureboundtech.dto.CertificateEligibilityDto;
import com.futureboundtech.security.CustomUserDetails;
import com.futureboundtech.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;

/**
 * Admin certificate management: search / approve / issue / revoke certificates
 * and configure the eligibility rules that gate issuance.
 */
@Controller
@RequestMapping("/admin/certificates")
@RequiredArgsConstructor
public class AdminCertificateController {

    private final CertificateService certificateService;

    // ------------------------------------------------------------------ list

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String status,
                       Model model) {
        model.addAttribute("pageTitle", "Certificates — Admin");
        model.addAttribute("certificates", certificateService.searchCertificates(q, status));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("courseOptions", certificateService.courseOptions());
        return "admin/certificates";
    }

    @PostMapping("/issue")
    public String issue(@RequestParam Long enrollmentId,
                        @AuthenticationPrincipal CustomUserDetails principal,
                        RedirectAttributes redirectAttributes) {
        var dto = certificateService.issue(enrollmentId, principal.getUser().getEmail());
        redirectAttributes.addFlashAttribute("successMessage",
                dto.isPendingApproval()
                        ? "Student is eligible — certificate queued for approval (" + dto.getCertificateNumber() + ")."
                        : "Certificate issued: " + dto.getCertificateNumber());
        return "redirect:/admin/certificates";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @AuthenticationPrincipal CustomUserDetails principal,
                          RedirectAttributes redirectAttributes) {
        certificateService.approve(id, principal.getUser().getEmail());
        redirectAttributes.addFlashAttribute("successMessage", "Certificate approved and issued.");
        return "redirect:/admin/certificates";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        certificateService.reject(id);
        redirectAttributes.addFlashAttribute("successMessage", "Pending certificate rejected.");
        return "redirect:/admin/certificates";
    }

    @PostMapping("/{id}/revoke")
    public String revoke(@PathVariable Long id,
                         @RequestParam(required = false) String reason,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes) {
        certificateService.revoke(id, reason, principal.getUser().getEmail());
        redirectAttributes.addFlashAttribute("successMessage", "Certificate revoked.");
        return "redirect:/admin/certificates";
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id,
                                           @AuthenticationPrincipal CustomUserDetails principal) {
        var dto = certificateService.getCertificate(id);
        byte[] pdf = certificateService.renderCertificatePdf(id, principal.getUser(), true);
        return pdfResponse(pdf, certificateService.pdfFileName(dto));
    }

    // ---------------------------------------------------------- eligibility

    @GetMapping("/eligibility")
    public String eligibility(@RequestParam(required = false) Long courseId, Model model) {
        model.addAttribute("pageTitle", "Certificate Eligibility — Admin");
        model.addAttribute("globalConfig", certificateService.getGlobalConfig());
        model.addAttribute("courseConfigs", certificateService.listConfigs().stream()
                .filter(c -> c.getCourseId() != null).toList());
        model.addAttribute("courseOptions", certificateService.courseOptions());
        model.addAttribute("quizOptions", certificateService.quizOptions());
        CertificateEligibilityDto form = courseId != null
                ? certificateService.getConfigForCourse(courseId)
                : certificateService.getGlobalConfig();
        model.addAttribute("form", form);
        model.addAttribute("editingCourseId", courseId);
        return "admin/certificate-eligibility";
    }

    @PostMapping("/eligibility")
    public String saveEligibility(@ModelAttribute("form") CertificateEligibilityDto form,
                                  RedirectAttributes redirectAttributes) {
        certificateService.saveConfig(form);
        redirectAttributes.addFlashAttribute("successMessage",
                form.getCourseId() == null
                        ? "Default eligibility rules saved."
                        : "Eligibility rules saved for " + form.getCourseTitle() + ".");
        return form.getCourseId() == null
                ? "redirect:/admin/certificates/eligibility"
                : "redirect:/admin/certificates/eligibility?courseId=" + form.getCourseId();
    }

    // -------------------------------------------------------------- issuing

    @GetMapping("/issue-preview")
    public String issuePreview(@RequestParam(required = false) Long courseId, Model model) {
        model.addAttribute("pageTitle", "Issue Certificates — Admin");
        model.addAttribute("courseOptions", certificateService.courseOptions());
        model.addAttribute("selectedCourseId", courseId);
        if (courseId != null) {
            model.addAttribute("evaluations", certificateService.evaluationsForCourse(courseId));
        }
        return "admin/certificate-issue";
    }

    @PostMapping("/issue-preview/scan")
    public String scan(@RequestParam Long courseId,
                       @AuthenticationPrincipal CustomUserDetails principal,
                       RedirectAttributes redirectAttributes) {
        int[] result = certificateService.scanCourse(courseId, principal.getUser().getEmail());
        redirectAttributes.addFlashAttribute("successMessage",
                "Scan complete: " + result[0] + " issued, " + result[1] + " queued for approval, "
                        + result[2] + " not yet eligible.");
        return "redirect:/admin/certificates/issue-preview?courseId=" + courseId;
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String fileName) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
