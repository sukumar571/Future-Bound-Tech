package com.futureboundtech.controller;

import com.futureboundtech.dto.QuizResultDto;
import com.futureboundtech.dto.StudentProfileFormDto;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.security.CustomUserDetails;
import com.futureboundtech.service.FileStorageService;
import com.futureboundtech.service.FutureMentorService;
import com.futureboundtech.service.NotificationService;
import com.futureboundtech.service.StudentDashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Student-facing dashboard and all of its sub-pages. Every handler resolves the
 * authenticated principal and delegates to {@link StudentDashboardService}, which
 * scopes all data to the current student so nobody can view another student's info.
 */
@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentDashboardService studentDashboardService;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final FutureMentorService futureMentorService;
    private final com.futureboundtech.service.CertificateService certificateService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pageTitle", "Student Dashboard — Future Bound Tech");
        model.addAttribute("dashboard", studentDashboardService.getDashboard(principal.getUser()));
        return "student/dashboard";
    }

    @GetMapping("/courses")
    public String courses(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pageTitle", "My Courses — Future Bound Tech");
        model.addAttribute("courses", studentDashboardService.getEnrolledCourses(principal.getUser()));
        return "student/courses";
    }

    @GetMapping("/courses/{id}")
    public String courseDetail(@PathVariable Long id,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               Model model) {
        model.addAttribute("pageTitle", "Course — Future Bound Tech");
        model.addAttribute("course", studentDashboardService.getCourseCard(principal.getUser(), id));
        return "student/course-detail";
    }

    @GetMapping("/syllabus")
    public String syllabusIndex(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pageTitle", "My Syllabus — Future Bound Tech");
        model.addAttribute("courses", studentDashboardService.getEnrolledCourses(principal.getUser()));
        return "student/syllabus-index";
    }

    @GetMapping("/classes")
    public String classes(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pageTitle", "Live Classes — Future Bound Tech");
        model.addAttribute("classes", studentDashboardService.getClasses(principal.getUser()));
        return "student/classes";
    }

    @GetMapping("/attendance")
    public String attendance(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pageTitle", "My Attendance — Future Bound Tech");
        model.addAttribute("attendance", studentDashboardService.getAttendance(principal.getUser()));
        return "student/attendance";
    }

    @GetMapping("/assignments")
    public String assignments(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pageTitle", "Assignments — Future Bound Tech");
        model.addAttribute("assignments", studentDashboardService.getAssignments(principal.getUser()));
        return "student/assignments";
    }

    @PostMapping("/assignments/{id}/submit")
    public String submitAssignment(@PathVariable Long id,
                                   @AuthenticationPrincipal CustomUserDetails principal,
                                   @RequestParam(value = "file", required = false) MultipartFile file,
                                   @RequestParam(value = "note", required = false) String note,
                                   RedirectAttributes redirectAttributes) {
        studentDashboardService.submitAssignment(principal.getUser(), id, file, note);
        redirectAttributes.addFlashAttribute("successMessage", "Your submission was received.");
        return "redirect:/student/assignments";
    }

    @GetMapping("/quizzes")
    public String quizzes(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pageTitle", "Quizzes — Future Bound Tech");
        model.addAttribute("quizzes", studentDashboardService.getQuizzes(principal.getUser()));
        return "student/quizzes";
    }

    @GetMapping("/quizzes/{id}/take")
    public String takeQuiz(@PathVariable Long id,
                           @AuthenticationPrincipal CustomUserDetails principal,
                           Model model) {
        model.addAttribute("pageTitle", "Take Quiz — Future Bound Tech");
        model.addAttribute("quiz", studentDashboardService.getQuizToTake(principal.getUser(), id));
        return "student/quiz-take";
    }

    @PostMapping("/quizzes/{id}/submit")
    public String submitQuiz(@PathVariable Long id,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             @RequestParam Map<String, String> params,
                             Model model) {
        Map<Long, String> answers = new HashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey().startsWith("answer-")) {
                try {
                    Long questionId = Long.valueOf(entry.getKey().substring("answer-".length()));
                    answers.put(questionId, entry.getValue());
                } catch (NumberFormatException ignored) {
                    // skip any stray parameter that is not a question answer
                }
            }
        }
        QuizResultDto result = studentDashboardService.submitQuiz(principal.getUser(), id, answers);
        model.addAttribute("pageTitle", "Quiz Result — Future Bound Tech");
        model.addAttribute("result", result);
        return "student/quiz-result";
    }

    @GetMapping("/payments")
    public String payments(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pageTitle", "Payments — Future Bound Tech");
        model.addAttribute("payments", studentDashboardService.getPayments(principal.getUser()));
        return "student/payments";
    }

    @GetMapping("/certificates")
    public String certificates(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pageTitle", "Certificates — Future Bound Tech");
        model.addAttribute("certificates", studentDashboardService.getCertificates(principal.getUser()));
        return "student/certificates";
    }

    @GetMapping("/certificates/{id}/download")
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable Long id,
                                                      @AuthenticationPrincipal CustomUserDetails principal) {
        var dto = certificateService.getCertificate(id);
        byte[] pdf = certificateService.renderCertificatePdf(id, principal.getUser(), false);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(certificateService.pdfFileName(dto), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/notifications")
    public String notifications(@RequestParam(value = "filter", required = false) String filter,
                                @AuthenticationPrincipal CustomUserDetails principal, Model model) {
        boolean unreadOnly = "unread".equalsIgnoreCase(filter);
        model.addAttribute("pageTitle", "Notifications — Future Bound Tech");
        model.addAttribute("notifications", notificationService.inbox(principal.getUser(), unreadOnly));
        model.addAttribute("unreadCount", notificationService.unreadCount(principal.getUser()));
        model.addAttribute("unreadOnly", unreadOnly);
        return "student/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String markRead(@PathVariable Long id,
                           @AuthenticationPrincipal CustomUserDetails principal,
                           RedirectAttributes redirectAttributes) {
        notificationService.markRead(principal.getUser(), id);
        redirectAttributes.addFlashAttribute("infoMessage", "Notification marked as read.");
        return "redirect:/student/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllRead(@AuthenticationPrincipal CustomUserDetails principal,
                              RedirectAttributes redirectAttributes) {
        notificationService.markAllRead(principal.getUser());
        redirectAttributes.addFlashAttribute("successMessage", "All notifications marked as read.");
        return "redirect:/student/notifications";
    }

    /** Dismissal is per-recipient: only this student's copy disappears. */
    @PostMapping("/notifications/{id}/delete")
    public String deleteNotification(@PathVariable Long id,
                                     @AuthenticationPrincipal CustomUserDetails principal,
                                     RedirectAttributes redirectAttributes) {
        notificationService.delete(principal.getUser(), id);
        redirectAttributes.addFlashAttribute("infoMessage", "Notification dismissed.");
        return "redirect:/student/notifications";
    }

    @GetMapping("/announcements")
    public String announcements(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pageTitle", "Announcements — Future Bound Tech");
        model.addAttribute("announcements", studentDashboardService.getAnnouncements(principal.getUser()));
        return "student/announcements";
    }

    /**
     * The Future Mentor chat page. The page itself is cheap to render — the
     * conversation is driven by the /api/student/mentor JSON endpoints — and it
     * always works, falling back to the offline study guide when AI is not set up.
     */
    @GetMapping("/mentor")
    public String mentor(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pageTitle", "Future Mentor — Future Bound Tech");
        model.addAttribute("featureEnabled", futureMentorService.isFeatureEnabled());
        model.addAttribute("aiConfigured", futureMentorService.isAiConfigured());
        model.addAttribute("historyEnabled", futureMentorService.isHistoryEnabled());
        return "student/mentor";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        var profile = studentDashboardService.getProfile(principal.getUser());
        model.addAttribute("pageTitle", "My Profile — Future Bound Tech");
        model.addAttribute("profile", profile);
        model.addAttribute("dashboard", studentDashboardService.getDashboard(principal.getUser()));
        model.addAttribute("profileForm", new StudentProfileFormDto().setEducation(profile.getEducation()));
        return "student/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails principal,
                                @Valid @ModelAttribute("profileForm") StudentProfileFormDto form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "My Profile — Future Bound Tech");
            model.addAttribute("profile", studentDashboardService.getProfile(principal.getUser()));
            model.addAttribute("dashboard", studentDashboardService.getDashboard(principal.getUser()));
            return "student/profile";
        }
        try {
            studentDashboardService.updateProfile(principal.getUser(), form);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/student/profile";
    }
}
