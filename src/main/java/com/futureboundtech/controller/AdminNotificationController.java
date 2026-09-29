package com.futureboundtech.controller;

import com.futureboundtech.dto.NotificationSendDto;
import com.futureboundtech.enums.NotificationAudience;
import com.futureboundtech.enums.NotificationType;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.service.AdminService;
import com.futureboundtech.service.EmailNotificationService;
import com.futureboundtech.service.NotificationReminderScheduler;
import com.futureboundtech.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Admin notification console: broadcast to an audience, review what was sent,
 * and check how the optional e-mail channel is configured.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminNotificationController {

    /** Types an admin may send by hand; the system-only types stay automatic. */
    private static final List<NotificationType> SENDABLE_TYPES = List.of(
            NotificationType.GENERAL_ANNOUNCEMENT,
            NotificationType.COURSE_ANNOUNCEMENT,
            NotificationType.BATCH_ANNOUNCEMENT,
            NotificationType.CLASS_REMINDER,
            NotificationType.ASSIGNMENT_DEADLINE,
            NotificationType.ENROLLMENT_CONFIRMATION,
            NotificationType.PAYMENT_CONFIRMATION,
            NotificationType.QUIZ_RESULT,
            NotificationType.CERTIFICATE_ISSUED,
            NotificationType.INFO,
            NotificationType.WARNING,
            NotificationType.ALERT,
            NotificationType.MESSAGE);

    private final NotificationService notificationService;
    private final EmailNotificationService emailNotificationService;
    private final NotificationReminderScheduler reminderScheduler;
    private final AdminService adminService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, true));
    }

    @GetMapping("/notifications")
    public String notifications(Model model) {
        model.addAttribute("pageTitle", "Notifications — Admin");
        model.addAttribute("notifications", notificationService.listAll());
        model.addAttribute("emailEnabled", emailNotificationService.isEnabled());
        model.addAttribute("emailStatus", emailNotificationService.statusReason());
        return "admin/notifications";
    }

    @GetMapping("/notifications/new")
    public String sendForm(Model model) {
        populateSendForm(model, new NotificationSendDto());
        return "admin/notification-send";
    }

    @PostMapping("/notifications")
    public String send(@Valid @ModelAttribute("sendForm") NotificationSendDto form,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateSendForm(model, form);
            model.addAttribute("errorMessage", "Please review the highlighted fields.");
            return "admin/notification-send";
        }
        try {
            int delivered = notificationService.send(form);
            String noun = form.trainerAudience() ? "trainer" : "student";
            redirectAttributes.addFlashAttribute("successMessage",
                    "Notification sent to " + delivered + " " + noun + "(s).");
            return "redirect:/admin/notifications";
        } catch (BusinessException ex) {
            populateSendForm(model, form);
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/notification-send";
        }
    }

    /** Runs the reminder job on demand so class/deadline notices can be checked immediately. */
    @PostMapping("/notifications/reminders")
    public String runReminders(RedirectAttributes redirectAttributes) {
        int created = reminderScheduler.runSweep();
        redirectAttributes.addFlashAttribute("successMessage", created == 0
                ? "No reminders were due."
                : "Created " + created + " reminder(s) for upcoming classes and deadlines.");
        return "redirect:/admin/notifications";
    }

    private void populateSendForm(Model model, NotificationSendDto form) {
        model.addAttribute("pageTitle", "Send Notification — Admin");
        model.addAttribute("sendForm", form);
        model.addAttribute("audiences", NotificationAudience.values());
        model.addAttribute("types", SENDABLE_TYPES);
        model.addAttribute("courses", adminService.listCourseOptions());
        model.addAttribute("batches", adminService.listBatches());
        model.addAttribute("students", adminService.listStudents(null, true));
        model.addAttribute("trainers", adminService.listTrainers(null, true));
        model.addAttribute("emailEnabled", emailNotificationService.isEnabled());
        model.addAttribute("emailStatus", emailNotificationService.statusReason());
    }
}
