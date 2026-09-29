package com.futureboundtech.controller;

import com.futureboundtech.dto.InstituteSettingsDto;
import com.futureboundtech.security.CustomUserDetails;
import com.futureboundtech.service.NotificationService;
import com.futureboundtech.service.WebsiteService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Attributes shared by every Thymeleaf view.
 * Replaces the removed #httpServletRequest object (Thymeleaf 3.1+).
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final NotificationService notificationService;
    private final WebsiteService websiteService;

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    /**
     * Fully-resolved institute settings (brand, theme colours, favicon, contact,
     * social, legal) available to every view for dynamic rendering.
     */
    @ModelAttribute("site")
    public InstituteSettingsDto site() {
        return websiteService.siteSettings();
    }

    /**
     * Unread badge for the navbar bell. Every role has an inbox, so the count is
     * read the same way regardless of who is signed in.
     */
    @ModelAttribute("navUnreadCount")
    public Long navUnreadCount(@AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null) {
            return null;
        }
        try {
            return notificationService.unreadCount(principal.getUser());
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
