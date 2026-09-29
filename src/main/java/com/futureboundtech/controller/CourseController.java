package com.futureboundtech.controller;

import com.futureboundtech.dto.CourseDto;
import com.futureboundtech.enums.CourseCategory;
import com.futureboundtech.enums.CourseLevel;
import com.futureboundtech.enums.CourseStatus;
import com.futureboundtech.enums.Role;
import com.futureboundtech.enums.TrainingMode;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.security.CustomUserDetails;
import com.futureboundtech.service.CourseService;
import com.futureboundtech.service.SyllabusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final SyllabusService syllabusService;
    private final com.futureboundtech.service.WebsiteService websiteService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(java.math.BigDecimal.class, new CustomNumberEditor(java.math.BigDecimal.class, true));
        binder.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, true));
    }

    @GetMapping("/courses")
    public String listPublic(@RequestParam(value = "q", required = false) String query,
                             @RequestParam(value = "category", required = false) CourseCategory category,
                             @RequestParam(value = "level", required = false) CourseLevel level,
                             Model model) {
        model.addAttribute("pageTitle", "Courses — Future Bound Tech");
        model.addAttribute("courses", courseService.searchPublic(query, category, level));
        model.addAttribute("q", query);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedLevel", level);
        addLookups(model);
        return "public/courses";
    }

    @GetMapping("/courses/{slug}")
    public String details(@PathVariable String slug,
                          @AuthenticationPrincipal CustomUserDetails principal,
                          Model model) {
        CourseDto course = courseService.findPublicBySlug(slug);
        model.addAttribute("pageTitle", course.getTitle() + " — Future Bound Tech");
        model.addAttribute("course", course);
        boolean isStudent = principal != null && principal.getUser().getRole() == Role.STUDENT;
        model.addAttribute("enrolled", principal != null && courseService.isStudentEnrolled(slug, principal.getUser()));
        if (isStudent) {
            model.addAttribute("enrollableBatches", courseService.findEnrollableBatches(slug));
        }
        model.addAttribute("syllabusPreview", syllabusService.getPublicPreview(course.getId()));
        return "public/course-details";
    }

    @PostMapping("/courses/{slug}/enroll")
    public String enroll(@PathVariable String slug,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         @RequestParam(value = "batchId", required = false) Long batchId,
                         RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            courseService.enrollStudent(slug, principal.getUser(), batchId);
            redirectAttributes.addFlashAttribute("successMessage", "You are enrolled. We will confirm batch and fee details with you.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/courses/" + slug;
    }

    @GetMapping("/admin/courses")
    public String adminList(@RequestParam(value = "q", required = false) String query,
                            @RequestParam(value = "category", required = false) CourseCategory category,
                            @RequestParam(value = "level", required = false) CourseLevel level,
                            @RequestParam(value = "status", required = false) CourseStatus status,
                            Model model) {
        model.addAttribute("pageTitle", "Manage Courses — Admin");
        model.addAttribute("courses", courseService.searchAdmin(query, category, level, status));
        model.addAttribute("q", query);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedLevel", level);
        model.addAttribute("selectedStatus", status);
        addLookups(model);
        return "admin/courses/list";
    }

    @GetMapping("/admin/courses/new")
    public String createForm(Model model) {
        CourseDto dto = new CourseDto();
        dto.setStatus(CourseStatus.DRAFT);
        dto.setCertificateEligible(true);
        dto.setDurationMonths(3);
        dto.setTrainingMode(TrainingMode.HYBRID);
        applyCourseDefaults(dto);
        model.addAttribute("pageTitle", "Create Course — Admin");
        model.addAttribute("courseDto", dto);
        model.addAttribute("formAction", "/admin/courses");
        model.addAttribute("editing", false);
        addAdminFormLookups(model);
        return "admin/courses/form";
    }

    /** Prefills the new-course form with the institute's configured course defaults. */
    private void applyCourseDefaults(CourseDto dto) {
        com.futureboundtech.dto.InstituteSettingsDto s = websiteService.siteSettings();
        if (s.getDefaultDurationMonths() != null && s.getDefaultDurationMonths() >= 1) {
            dto.setDurationMonths(s.getDefaultDurationMonths());
        }
        if (s.getDefaultLevel() != null) {
            dto.setLevel(s.getDefaultLevel());
        }
        if (s.getDefaultTrainingMode() != null) {
            dto.setTrainingMode(s.getDefaultTrainingMode());
        }
        dto.setCertificateEligible(s.isDefaultCertificateEligible());
        dto.setPublicListed(s.isDefaultPublicListed());
    }

    @PostMapping("/admin/courses")
    public String create(@Valid @ModelAttribute("courseDto") CourseDto courseDto,
                         BindingResult bindingResult,
                         @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Create Course — Admin");
            model.addAttribute("formAction", "/admin/courses");
            model.addAttribute("editing", false);
            addAdminFormLookups(model);
            return "admin/courses/form";
        }
        try {
            CourseDto saved = courseService.create(courseDto, thumbnailFile);
            redirectAttributes.addFlashAttribute("successMessage", "Course created.");
            return "redirect:/admin/courses/" + saved.getId();
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pageTitle", "Create Course — Admin");
            model.addAttribute("formAction", "/admin/courses");
            model.addAttribute("editing", false);
            addAdminFormLookups(model);
            return "admin/courses/form";
        }
    }

    @GetMapping("/admin/courses/{id}")
    public String view(@PathVariable Long id, Model model) {
        CourseDto course = courseService.findAdminById(id);
        model.addAttribute("pageTitle", course.getTitle() + " — Admin");
        model.addAttribute("course", course);
        return "admin/courses/view";
    }

    @GetMapping("/admin/courses/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        CourseDto course = courseService.findAdminById(id);
        model.addAttribute("pageTitle", "Edit Course — Admin");
        model.addAttribute("courseDto", course);
        model.addAttribute("formAction", "/admin/courses/" + id);
        model.addAttribute("editing", true);
        addAdminFormLookups(model);
        return "admin/courses/form";
    }

    @PostMapping("/admin/courses/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("courseDto") CourseDto courseDto,
                         BindingResult bindingResult,
                         @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Edit Course — Admin");
            model.addAttribute("formAction", "/admin/courses/" + id);
            model.addAttribute("editing", true);
            addAdminFormLookups(model);
            return "admin/courses/form";
        }
        try {
            courseService.update(id, courseDto, thumbnailFile);
            redirectAttributes.addFlashAttribute("successMessage", "Course updated.");
            return "redirect:/admin/courses/" + id;
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pageTitle", "Edit Course — Admin");
            model.addAttribute("formAction", "/admin/courses/" + id);
            model.addAttribute("editing", true);
            addAdminFormLookups(model);
            return "admin/courses/form";
        }
    }

    @PostMapping("/admin/courses/{id}/publish")
    public String publish(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        courseService.publish(id);
        redirectAttributes.addFlashAttribute("successMessage", "Course published and listed publicly.");
        return "redirect:/admin/courses/" + id;
    }

    @PostMapping("/admin/courses/{id}/unpublish")
    public String unpublish(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        courseService.unpublish(id);
        redirectAttributes.addFlashAttribute("successMessage", "Course unpublished and hidden from the catalog.");
        return "redirect:/admin/courses/" + id;
    }

    @PostMapping("/admin/courses/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            courseService.deleteSafely(id);
            redirectAttributes.addFlashAttribute("successMessage", "Course deleted.");
            return "redirect:/admin/courses";
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/courses/" + id;
        }
    }

    private void addLookups(Model model) {
        model.addAttribute("categories", CourseCategory.values());
        model.addAttribute("levels", CourseLevel.values());
        model.addAttribute("statuses", CourseStatus.values());
        model.addAttribute("trainingModes", TrainingMode.values());
    }

    private void addAdminFormLookups(Model model) {
        addLookups(model);
        model.addAttribute("trainers", courseService.listTrainers());
    }
}
