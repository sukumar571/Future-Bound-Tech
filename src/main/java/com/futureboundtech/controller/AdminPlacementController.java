package com.futureboundtech.controller;

import com.futureboundtech.dto.PlacementFormDto;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.service.PlacementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Admin management of placement posts (Phase 17): create/edit, publish/unpublish,
 * delete. Scoped to /admin/** (ROLE ADMIN).
 */
@Controller
@RequestMapping("/admin/placements")
@RequiredArgsConstructor
public class AdminPlacementController {

    private static final List<String> JOB_TYPES =
            List.of("Full-time", "Internship", "Contract", "Part-time", "Remote");

    private final PlacementService placementService;

    @GetMapping({"", "/"})
    public String placements(Model model) {
        model.addAttribute("pageTitle", "Placement Posts — Admin");
        model.addAttribute("placements", placementService.listAll());
        return "admin/placements";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        populateForm(model, new PlacementFormDto(), false, null);
        return "admin/placement-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        populateForm(model, placementService.getPlacementForm(id), true, id);
        return "admin/placement-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("placementForm") PlacementFormDto form,
                         BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, form, false, null);
            model.addAttribute("errorMessage", "Please review the highlighted fields.");
            return "admin/placement-form";
        }
        try {
            placementService.createPlacement(form);
            redirectAttributes.addFlashAttribute("successMessage", "Placement post created.");
            return "redirect:/admin/placements";
        } catch (BusinessException ex) {
            populateForm(model, form, false, null);
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/placement-form";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("placementForm") PlacementFormDto form,
                         BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model, form, true, id);
            model.addAttribute("errorMessage", "Please review the highlighted fields.");
            return "admin/placement-form";
        }
        try {
            placementService.updatePlacement(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Placement post updated.");
            return "redirect:/admin/placements";
        } catch (BusinessException ex) {
            populateForm(model, form, true, id);
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/placement-form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        placementService.deletePlacement(id);
        redirectAttributes.addFlashAttribute("successMessage", "Placement post deleted.");
        return "redirect:/admin/placements";
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        placementService.setPublished(id, true);
        redirectAttributes.addFlashAttribute("successMessage", "Placement post published.");
        return "redirect:/admin/placements";
    }

    @PostMapping("/{id}/unpublish")
    public String unpublish(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        placementService.setPublished(id, false);
        redirectAttributes.addFlashAttribute("successMessage", "Placement post unpublished.");
        return "redirect:/admin/placements";
    }

    private void populateForm(Model model, PlacementFormDto form, boolean editing, Long id) {
        model.addAttribute("pageTitle", (editing ? "Edit Placement Post" : "New Placement Post") + " — Admin");
        model.addAttribute("placementForm", form);
        model.addAttribute("editing", editing);
        model.addAttribute("formAction", editing ? "/admin/placements/" + id : "/admin/placements");
        model.addAttribute("jobTypes", JOB_TYPES);
    }
}
