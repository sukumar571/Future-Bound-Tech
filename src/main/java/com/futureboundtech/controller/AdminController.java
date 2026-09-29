package com.futureboundtech.controller;

import com.futureboundtech.dto.*;
import com.futureboundtech.enums.CourseCategory;
import com.futureboundtech.enums.CourseLevel;
import com.futureboundtech.enums.CourseStatus;
import com.futureboundtech.enums.TrainingMode;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.service.AdminService;
import com.futureboundtech.service.CourseService;
import com.futureboundtech.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final CourseService courseService;
    private final PaymentService paymentService;

    // -----------------------------------------------------------------
    // Dashboard
    // -----------------------------------------------------------------
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Admin Dashboard — Future Bound Tech");
        model.addAttribute("stats", adminService.dashboardStats());
        return "admin/dashboard";
    }

    // -----------------------------------------------------------------
    // Syllabus (managed per course — entry point lists courses)
    // -----------------------------------------------------------------
    @GetMapping("/syllabus")
    public String syllabus() {
        return "redirect:/admin/courses";
    }

    // -----------------------------------------------------------------
    // Reports
    // -----------------------------------------------------------------
    @GetMapping("/reports")
    public String reports(@RequestParam(value = "status", required = false) com.futureboundtech.enums.PaymentStatus status,
                          Model model) {
        model.addAttribute("pageTitle", "Reports — Admin");
        model.addAttribute("stats", adminService.dashboardStats());
        model.addAttribute("summary", paymentService.adminSummary());
        model.addAttribute("payments", paymentService.adminReport(status));
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", com.futureboundtech.enums.PaymentStatus.values());
        return "admin/reports";
    }

    // -----------------------------------------------------------------
    // Students
    // -----------------------------------------------------------------
    @GetMapping("/students")
    public String students(@RequestParam(value = "q", required = false) String query,
                           @RequestParam(value = "active", required = false) Boolean active,
                           Model model) {
        model.addAttribute("pageTitle", "Students — Admin");
        model.addAttribute("students", adminService.listStudents(query, active));
        model.addAttribute("q", query);
        model.addAttribute("selectedActive", active);
        return "admin/students";
    }

    @GetMapping("/students/new")
    public String newStudentForm(Model model) {
        model.addAttribute("pageTitle", "Add Student — Admin");
        model.addAttribute("studentForm", new StudentFormDto());
        model.addAttribute("editing", false);
        model.addAttribute("formAction", "/admin/students");
        return "admin/student-form";
    }

    @PostMapping("/students")
    public String createStudent(@Valid @ModelAttribute("studentForm") StudentFormDto form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return renderStudentForm(model, form, false, null, firstError(bindingResult));
        }
        try {
            adminService.createStudent(form);
            redirectAttributes.addFlashAttribute("successMessage", "Student created.");
            return "redirect:/admin/students";
        } catch (BusinessException ex) {
            return renderStudentForm(model, form, false, null, ex.getMessage());
        }
    }

    @GetMapping("/students/{id}")
    public String studentDetail(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "Student Profile — Admin");
        model.addAttribute("student", adminService.getStudent(id));
        model.addAttribute("enrollments", adminService.studentEnrollments(id));
        model.addAttribute("payments", adminService.studentPayments(id));
        return "admin/student-detail";
    }

    @GetMapping("/students/{id}/edit")
    public String editStudentForm(@PathVariable Long id, Model model) {
        StudentDto student = adminService.getStudent(id);
        StudentFormDto form = new StudentFormDto()
                .setUserId(student.getUserId())
                .setFirstName(student.getFirstName())
                .setLastName(student.getLastName())
                .setEmail(student.getEmail())
                .setPhone(student.getPhone())
                .setEducation(student.getEducation())
                .setActive(student.isActive());
        model.addAttribute("pageTitle", "Edit Student — Admin");
        model.addAttribute("studentForm", form);
        model.addAttribute("editing", true);
        model.addAttribute("studentId", id);
        model.addAttribute("formAction", "/admin/students/" + id);
        return "admin/student-form";
    }

    @PostMapping("/students/{id}")
    public String updateStudent(@PathVariable Long id,
                                @Valid @ModelAttribute("studentForm") StudentFormDto form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return renderStudentForm(model, form, true, id, firstError(bindingResult));
        }
        try {
            adminService.updateStudent(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Student updated.");
            return "redirect:/admin/students/" + id;
        } catch (BusinessException ex) {
            return renderStudentForm(model, form, true, id, ex.getMessage());
        }
    }

    @PostMapping("/students/{id}/status")
    public String toggleStudent(@PathVariable Long id,
                                @RequestParam("active") boolean active,
                                RedirectAttributes redirectAttributes) {
        adminService.setStudentActive(id, active);
        redirectAttributes.addFlashAttribute("successMessage", active ? "Student activated." : "Student deactivated.");
        return "redirect:/admin/students";
    }

    // -----------------------------------------------------------------
    // Trainers
    // -----------------------------------------------------------------
    @GetMapping("/trainers")
    public String trainers(@RequestParam(value = "q", required = false) String query,
                           @RequestParam(value = "active", required = false) Boolean active,
                           Model model) {
        model.addAttribute("pageTitle", "Trainers — Admin");
        model.addAttribute("trainers", adminService.listTrainers(query, active));
        model.addAttribute("q", query);
        model.addAttribute("selectedActive", active);
        return "admin/trainers";
    }

    @GetMapping("/trainers/new")
    public String newTrainerForm(Model model) {
        model.addAttribute("pageTitle", "Add Trainer — Admin");
        model.addAttribute("trainerForm", new TrainerFormDto());
        model.addAttribute("editing", false);
        model.addAttribute("formAction", "/admin/trainers");
        return "admin/trainer-form";
    }

    @PostMapping("/trainers")
    public String createTrainer(@Valid @ModelAttribute("trainerForm") TrainerFormDto form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return renderTrainerForm(model, form, false, null, firstError(bindingResult));
        }
        try {
            adminService.createTrainer(form);
            redirectAttributes.addFlashAttribute("successMessage", "Trainer created.");
            return "redirect:/admin/trainers";
        } catch (BusinessException ex) {
            return renderTrainerForm(model, form, false, null, ex.getMessage());
        }
    }

    @GetMapping("/trainers/{id}/edit")
    public String editTrainerForm(@PathVariable Long id, Model model) {
        TrainerDto trainer = adminService.getTrainer(id);
        TrainerFormDto form = new TrainerFormDto()
                .setUserId(trainer.getUserId())
                .setFirstName(trainer.getFirstName())
                .setLastName(trainer.getLastName())
                .setEmail(trainer.getEmail())
                .setPhone(trainer.getPhone())
                .setExpertise(trainer.getExpertise())
                .setBio(trainer.getBio())
                .setExperienceYears(trainer.getExperienceYears())
                .setPhotoUrl(trainer.getPhotoUrl())
                .setActive(trainer.isActive());
        model.addAttribute("pageTitle", "Edit Trainer — Admin");
        model.addAttribute("trainerForm", form);
        model.addAttribute("editing", true);
        model.addAttribute("trainerId", id);
        model.addAttribute("formAction", "/admin/trainers/" + id);
        return "admin/trainer-form";
    }

    @PostMapping("/trainers/{id}")
    public String updateTrainer(@PathVariable Long id,
                                @Valid @ModelAttribute("trainerForm") TrainerFormDto form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return renderTrainerForm(model, form, true, id, firstError(bindingResult));
        }
        try {
            adminService.updateTrainer(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Trainer updated.");
            return "redirect:/admin/trainers";
        } catch (BusinessException ex) {
            return renderTrainerForm(model, form, true, id, ex.getMessage());
        }
    }

    @PostMapping("/trainers/{id}/status")
    public String toggleTrainer(@PathVariable Long id,
                                @RequestParam("active") boolean active,
                                RedirectAttributes redirectAttributes) {
        adminService.setTrainerActive(id, active);
        redirectAttributes.addFlashAttribute("successMessage", active ? "Trainer activated." : "Trainer deactivated.");
        return "redirect:/admin/trainers";
    }

    // -----------------------------------------------------------------
    // Settings
    // -----------------------------------------------------------------
    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("pageTitle", "Settings — Admin");
        model.addAttribute("settings", adminService.getSettings());
        populateSettingsLookups(model);
        return "admin/settings";
    }

    @PostMapping("/settings")
    public String saveSettings(@Valid @ModelAttribute("settings") InstituteSettingsDto dto,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Settings — Admin");
            model.addAttribute("errorMessage", firstError(bindingResult));
            populateSettingsLookups(model);
            return "admin/settings";
        }
        adminService.saveSettings(dto);
        redirectAttributes.addFlashAttribute("successMessage", "Institute settings saved.");
        return "redirect:/admin/settings";
    }

    /** Enum option lists for the settings form's course-default dropdowns. */
    private void populateSettingsLookups(Model model) {
        model.addAttribute("courseLevels", com.futureboundtech.enums.CourseLevel.values());
        model.addAttribute("trainingModes", com.futureboundtech.enums.TrainingMode.values());
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------
    private String renderStudentForm(Model model, StudentFormDto form, boolean editing, Long id, String error) {
        model.addAttribute("pageTitle", (editing ? "Edit Student" : "Add Student") + " — Admin");
        model.addAttribute("studentForm", form);
        model.addAttribute("editing", editing);
        model.addAttribute("studentId", id);
        model.addAttribute("formAction", editing ? "/admin/students/" + id : "/admin/students");
        model.addAttribute("errorMessage", error);
        return "admin/student-form";
    }

    private String renderTrainerForm(Model model, TrainerFormDto form, boolean editing, Long id, String error) {
        model.addAttribute("pageTitle", (editing ? "Edit Trainer" : "Add Trainer") + " — Admin");
        model.addAttribute("trainerForm", form);
        model.addAttribute("editing", editing);
        model.addAttribute("trainerId", id);
        model.addAttribute("formAction", editing ? "/admin/trainers/" + id : "/admin/trainers");
        model.addAttribute("errorMessage", error);
        return "admin/trainer-form";
    }

    private String firstError(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "Please review the highlighted fields." : error.getDefaultMessage())
                .orElse("Please review the highlighted fields.");
    }
}
