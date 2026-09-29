package com.futureboundtech.controller;

import com.futureboundtech.dto.BatchDto;
import com.futureboundtech.dto.LiveClassDto;
import com.futureboundtech.enums.BatchEnrollmentStatus;
import com.futureboundtech.enums.ClassMode;
import com.futureboundtech.enums.ClassStatus;
import com.futureboundtech.enums.EnrollmentStatus;
import com.futureboundtech.enums.TrainingMode;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.service.AdminService;
import com.futureboundtech.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminAcademicsController {

    private final AdminService adminService;
    private final CourseService courseService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, true));
    }

    // -----------------------------------------------------------------
    // Batches
    // -----------------------------------------------------------------
    @GetMapping("/batches")
    public String batches(Model model) {
        model.addAttribute("pageTitle", "Batches — Admin");
        model.addAttribute("batches", adminService.listBatches());
        return "admin/batches";
    }

    @GetMapping("/batches/new")
    public String newBatchForm(Model model) {
        BatchDto dto = new BatchDto();
        dto.setMode(TrainingMode.HYBRID);
        dto.setStatus(ClassStatus.SCHEDULED);
        dto.setMaxSeats(30);
        populateBatchForm(model, dto, false, null);
        return "admin/batch-form";
    }

    @PostMapping("/batches")
    public String createBatch(@Valid @ModelAttribute("batchDto") BatchDto dto,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateBatchForm(model, dto, false, null);
            model.addAttribute("errorMessage", "Please review the highlighted fields.");
            return "admin/batch-form";
        }
        try {
            adminService.createBatch(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Batch created.");
            return "redirect:/admin/batches";
        } catch (BusinessException ex) {
            populateBatchForm(model, dto, false, null);
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/batch-form";
        }
    }

    @GetMapping("/batches/{id}/edit")
    public String editBatchForm(@PathVariable Long id, Model model) {
        populateBatchForm(model, adminService.getBatch(id), true, id);
        return "admin/batch-form";
    }

    @PostMapping("/batches/{id}")
    public String updateBatch(@PathVariable Long id,
                              @Valid @ModelAttribute("batchDto") BatchDto dto,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateBatchForm(model, dto, true, id);
            model.addAttribute("errorMessage", "Please review the highlighted fields.");
            return "admin/batch-form";
        }
        try {
            adminService.updateBatch(id, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Batch updated.");
            return "redirect:/admin/batches";
        } catch (BusinessException ex) {
            populateBatchForm(model, dto, true, id);
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/batch-form";
        }
    }

    @PostMapping("/batches/{id}/delete")
    public String deleteBatch(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteBatch(id);
            redirectAttributes.addFlashAttribute("successMessage", "Batch deleted.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/batches";
    }

    @PostMapping("/batches/{id}/publish")
    public String publishBatch(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminService.setBatchPublished(id, true);
        redirectAttributes.addFlashAttribute("successMessage", "Batch published.");
        return "redirect:/admin/batches";
    }

    @PostMapping("/batches/{id}/unpublish")
    public String unpublishBatch(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminService.setBatchPublished(id, false);
        redirectAttributes.addFlashAttribute("successMessage", "Batch unpublished — hidden from students.");
        return "redirect:/admin/batches";
    }

    @PostMapping("/batches/{id}/enrollment")
    public String setBatchEnrollment(@PathVariable Long id,
                                     @RequestParam("status") BatchEnrollmentStatus status,
                                     RedirectAttributes redirectAttributes) {
        adminService.setBatchEnrollmentStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage",
                status == BatchEnrollmentStatus.CLOSED ? "Enrollment closed." : "Enrollment opened.");
        return "redirect:/admin/batches";
    }

    // -----------------------------------------------------------------
    // Live classes
    // -----------------------------------------------------------------
    @GetMapping("/live-classes")
    public String liveClasses(Model model) {
        model.addAttribute("pageTitle", "Live Classes — Admin");
        model.addAttribute("liveClasses", adminService.listLiveClasses());
        return "admin/live-classes";
    }

    @GetMapping("/live-classes/new")
    public String newLiveClassForm(Model model) {
        LiveClassDto dto = new LiveClassDto();
        dto.setStatus(ClassStatus.SCHEDULED);
        populateLiveClassForm(model, dto, false, null);
        return "admin/live-class-form";
    }

    @PostMapping("/live-classes")
    public String createLiveClass(@Valid @ModelAttribute("liveClassDto") LiveClassDto dto,
                                  BindingResult bindingResult,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateLiveClassForm(model, dto, false, null);
            model.addAttribute("errorMessage", "Please review the highlighted fields.");
            return "admin/live-class-form";
        }
        try {
            adminService.createLiveClass(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Live class scheduled.");
            return "redirect:/admin/live-classes";
        } catch (BusinessException ex) {
            populateLiveClassForm(model, dto, false, null);
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/live-class-form";
        }
    }

    @GetMapping("/live-classes/{id}/edit")
    public String editLiveClassForm(@PathVariable Long id, Model model) {
        populateLiveClassForm(model, adminService.getLiveClass(id), true, id);
        return "admin/live-class-form";
    }

    @PostMapping("/live-classes/{id}")
    public String updateLiveClass(@PathVariable Long id,
                                  @Valid @ModelAttribute("liveClassDto") LiveClassDto dto,
                                  BindingResult bindingResult,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateLiveClassForm(model, dto, true, id);
            model.addAttribute("errorMessage", "Please review the highlighted fields.");
            return "admin/live-class-form";
        }
        try {
            adminService.updateLiveClass(id, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Live class updated.");
            return "redirect:/admin/live-classes";
        } catch (BusinessException ex) {
            populateLiveClassForm(model, dto, true, id);
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/live-class-form";
        }
    }

    @PostMapping("/live-classes/{id}/delete")
    public String deleteLiveClass(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminService.deleteLiveClass(id);
        redirectAttributes.addFlashAttribute("successMessage", "Live class deleted.");
        return "redirect:/admin/live-classes";
    }

    // -----------------------------------------------------------------
    // Enrollments
    // -----------------------------------------------------------------
    @GetMapping("/enrollments")
    public String enrollments(@RequestParam(value = "q", required = false) String query,
                              @RequestParam(value = "status", required = false) EnrollmentStatus status,
                              Model model) {
        model.addAttribute("pageTitle", "Enrollments — Admin");
        model.addAttribute("enrollments", adminService.listEnrollments(query, status));
        model.addAttribute("q", query);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", EnrollmentStatus.values());
        return "admin/enrollments";
    }

    @PostMapping("/enrollments/{id}/status")
    public String updateEnrollmentStatus(@PathVariable Long id,
                                         @RequestParam("status") EnrollmentStatus status,
                                         RedirectAttributes redirectAttributes) {
        adminService.setEnrollmentStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage", "Enrollment status updated.");
        return "redirect:/admin/enrollments";
    }

    // -----------------------------------------------------------------
    // Form population helpers
    // -----------------------------------------------------------------
    private void populateBatchForm(Model model, BatchDto dto, boolean editing, Long id) {
        model.addAttribute("pageTitle", (editing ? "Edit Batch" : "New Batch") + " — Admin");
        model.addAttribute("batchDto", dto);
        model.addAttribute("editing", editing);
        model.addAttribute("batchId", id);
        model.addAttribute("formAction", editing ? "/admin/batches/" + id : "/admin/batches");
        model.addAttribute("courses", courseService.searchAdmin(null, null, null, null));
        model.addAttribute("trainers", courseService.listTrainers());
        model.addAttribute("trainingModes", TrainingMode.values());
        model.addAttribute("statuses", ClassStatus.values());
        model.addAttribute("enrollmentStatuses", BatchEnrollmentStatus.values());
    }

    private void populateLiveClassForm(Model model, LiveClassDto dto, boolean editing, Long id) {
        model.addAttribute("pageTitle", (editing ? "Edit Live Class" : "New Live Class") + " — Admin");
        model.addAttribute("liveClassDto", dto);
        model.addAttribute("editing", editing);
        model.addAttribute("liveClassId", id);
        model.addAttribute("formAction", editing ? "/admin/live-classes/" + id : "/admin/live-classes");
        model.addAttribute("batches", adminService.listBatches());
        model.addAttribute("modes", ClassMode.values());
        model.addAttribute("statuses", ClassStatus.values());
    }
}
