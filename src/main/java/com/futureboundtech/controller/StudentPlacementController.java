package com.futureboundtech.controller;

import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.security.CustomUserDetails;
import com.futureboundtech.service.PlacementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Student placement preparation area (Phase 17): browse published opportunities,
 * filter by skill, view eligibility + preparation materials, apply via the
 * authorized external link and track saved opportunities.
 * Scoped to /student/** (ROLE STUDENT).
 */
@Controller
@RequestMapping("/student/placements")
@RequiredArgsConstructor
public class StudentPlacementController {

    private final PlacementService placementService;

    @GetMapping({"", "/"})
    public String browse(@RequestParam(required = false) String skill,
                         @RequestParam(required = false, defaultValue = "false") boolean saved,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         Model model) {
        model.addAttribute("pageTitle", "Placement Opportunities — Future Bound Tech");
        model.addAttribute("placements",
                placementService.listOpportunities(principal.getUser(), skill, saved));
        model.addAttribute("skills", placementService.listDistinctSkills());
        model.addAttribute("fSkill", skill);
        model.addAttribute("fSaved", saved);
        return "student/placements";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         Model model) {
        model.addAttribute("pageTitle", "Opportunity — Future Bound Tech");
        model.addAttribute("placement", placementService.getOpportunity(principal.getUser(), id));
        return "student/placement-detail";
    }

    @PostMapping("/{id}/save")
    public String toggleSave(@PathVariable Long id,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             RedirectAttributes redirectAttributes) {
        try {
            boolean nowSaved = placementService.toggleSave(principal.getUser(), id);
            redirectAttributes.addFlashAttribute("successMessage",
                    nowSaved ? "Opportunity saved." : "Opportunity removed from saved.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/student/placements/" + id;
    }
}
