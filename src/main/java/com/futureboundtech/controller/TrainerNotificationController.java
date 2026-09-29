package com.futureboundtech.controller;

import com.futureboundtech.security.CustomUserDetails;
import com.futureboundtech.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Trainer inbox. Trainers receive the same kind of notifications students do,
 * and every handler here is scoped to the signed-in trainer's own rows.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/trainer")
public class TrainerNotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notifications")
    public String notifications(@RequestParam(value = "filter", required = false) String filter,
                                @AuthenticationPrincipal CustomUserDetails principal, Model model) {
        boolean unreadOnly = "unread".equalsIgnoreCase(filter);
        model.addAttribute("pageTitle", "Notifications — Trainer");
        model.addAttribute("notifications", notificationService.inbox(principal.getUser(), unreadOnly));
        model.addAttribute("unreadCount", notificationService.unreadCount(principal.getUser()));
        model.addAttribute("unreadOnly", unreadOnly);
        return "trainer/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String markRead(@PathVariable Long id,
                           @AuthenticationPrincipal CustomUserDetails principal,
                           RedirectAttributes redirectAttributes) {
        notificationService.markRead(principal.getUser(), id);
        redirectAttributes.addFlashAttribute("infoMessage", "Notification marked as read.");
        return "redirect:/trainer/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllRead(@AuthenticationPrincipal CustomUserDetails principal,
                              RedirectAttributes redirectAttributes) {
        notificationService.markAllRead(principal.getUser());
        redirectAttributes.addFlashAttribute("successMessage", "All notifications marked as read.");
        return "redirect:/trainer/notifications";
    }

    @PostMapping("/notifications/{id}/delete")
    public String deleteNotification(@PathVariable Long id,
                                     @AuthenticationPrincipal CustomUserDetails principal,
                                     RedirectAttributes redirectAttributes) {
        notificationService.delete(principal.getUser(), id);
        redirectAttributes.addFlashAttribute("infoMessage", "Notification dismissed.");
        return "redirect:/trainer/notifications";
    }
}
