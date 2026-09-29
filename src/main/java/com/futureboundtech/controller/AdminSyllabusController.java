package com.futureboundtech.controller;

import com.futureboundtech.dto.CourseModuleDto;
import com.futureboundtech.dto.LessonDto;
import com.futureboundtech.dto.LessonResourceDto;
import com.futureboundtech.enums.LessonType;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.service.CourseService;
import com.futureboundtech.service.SyllabusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminSyllabusController {

    private final SyllabusService syllabusService;
    private final CourseService courseService;

    @GetMapping("/admin/courses/{courseId}/syllabus")
    public String manage(@PathVariable Long courseId, Model model) {
        model.addAttribute("pageTitle", "Syllabus — Admin");
        model.addAttribute("course", courseService.findById(courseId));
        model.addAttribute("modules", syllabusService.getAdminSyllabus(courseId));
        return "admin/syllabus/manage";
    }

    @PostMapping("/admin/courses/{courseId}/modules")
    public String createModule(@PathVariable Long courseId,
                               @Valid @ModelAttribute("moduleDto") CourseModuleDto moduleDto,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstFieldError(bindingResult));
            return "redirect:/admin/courses/" + courseId + "/syllabus";
        }
        try {
            syllabusService.createModule(courseId, moduleDto);
            redirectAttributes.addFlashAttribute("successMessage", "Module created.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/courses/" + courseId + "/syllabus";
    }

    @PostMapping("/admin/modules/{moduleId}/edit")
    public String updateModule(@PathVariable Long moduleId,
                               @Valid @ModelAttribute("moduleDto") CourseModuleDto moduleDto,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        CourseModuleDto module = syllabusService.getModuleForAdmin(moduleId);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstFieldError(bindingResult));
            return "redirect:/admin/courses/" + module.getCourseId() + "/syllabus";
        }
        try {
            syllabusService.updateModule(moduleId, moduleDto);
            redirectAttributes.addFlashAttribute("successMessage", "Module updated.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/courses/" + module.getCourseId() + "/syllabus";
    }

    @PostMapping("/admin/modules/{moduleId}/delete")
    public String deleteModule(@PathVariable Long moduleId, RedirectAttributes redirectAttributes) {
        Long courseId = syllabusService.deleteModule(moduleId);
        redirectAttributes.addFlashAttribute("successMessage", "Module deleted with its lessons and resources.");
        return "redirect:/admin/courses/" + courseId + "/syllabus";
    }

    @PostMapping("/admin/modules/{moduleId}/move")
    public String moveModule(@PathVariable Long moduleId, @RequestParam("direction") String direction) {
        Long courseId = syllabusService.moveModule(moduleId, "down".equalsIgnoreCase(direction) ? 1 : -1);
        return "redirect:/admin/courses/" + courseId + "/syllabus";
    }

    @GetMapping("/admin/modules/{moduleId}/lessons/new")
    public String newLessonForm(@PathVariable Long moduleId, Model model) {
        CourseModuleDto module = syllabusService.getModuleForAdmin(moduleId);
        LessonDto lessonDto = new LessonDto();
        lessonDto.setLessonType(LessonType.READING);
        lessonDto.setDurationMinutes(45);
        populateLessonForm(model, lessonDto, "/admin/modules/" + moduleId + "/lessons", false, module);
        return "admin/syllabus/lesson-form";
    }

    @PostMapping("/admin/modules/{moduleId}/lessons")
    public String createLesson(@PathVariable Long moduleId,
                               @Valid @ModelAttribute("lessonDto") LessonDto lessonDto,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        CourseModuleDto module = syllabusService.getModuleForAdmin(moduleId);
        if (bindingResult.hasErrors()) {
            populateLessonForm(model, lessonDto, "/admin/modules/" + moduleId + "/lessons", false, module);
            return "admin/syllabus/lesson-form";
        }
        try {
            LessonDto saved = syllabusService.createLesson(moduleId, lessonDto);
            redirectAttributes.addFlashAttribute("successMessage", "Lesson created.");
            return "redirect:/admin/courses/" + saved.getCourseId() + "/syllabus";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateLessonForm(model, lessonDto, "/admin/modules/" + moduleId + "/lessons", false, module);
            return "admin/syllabus/lesson-form";
        }
    }

    @GetMapping("/admin/lessons/{lessonId}/edit")
    public String editLessonForm(@PathVariable Long lessonId, Model model) {
        LessonDto lesson = syllabusService.getLessonForEdit(lessonId);
        CourseModuleDto module = syllabusService.getModuleForAdmin(lesson.getModuleId());
        populateLessonForm(model, lesson, "/admin/lessons/" + lessonId + "/edit", true, module);
        return "admin/syllabus/lesson-form";
    }

    @PostMapping("/admin/lessons/{lessonId}/edit")
    public String updateLesson(@PathVariable Long lessonId,
                               @Valid @ModelAttribute("lessonDto") LessonDto lessonDto,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        LessonDto existing = syllabusService.getLessonForEdit(lessonId);
        CourseModuleDto module = syllabusService.getModuleForAdmin(existing.getModuleId());
        if (bindingResult.hasErrors()) {
            lessonDto.setResources(existing.getResources());
            populateLessonForm(model, lessonDto, "/admin/lessons/" + lessonId + "/edit", true, module);
            return "admin/syllabus/lesson-form";
        }
        try {
            LessonDto saved = syllabusService.updateLesson(lessonId, lessonDto);
            redirectAttributes.addFlashAttribute("successMessage", "Lesson updated.");
            return "redirect:/admin/courses/" + saved.getCourseId() + "/syllabus";
        } catch (BusinessException ex) {
            lessonDto.setResources(existing.getResources());
            model.addAttribute("errorMessage", ex.getMessage());
            populateLessonForm(model, lessonDto, "/admin/lessons/" + lessonId + "/edit", true, module);
            return "admin/syllabus/lesson-form";
        }
    }

    @PostMapping("/admin/lessons/{lessonId}/delete")
    public String deleteLesson(@PathVariable Long lessonId, RedirectAttributes redirectAttributes) {
        Long courseId = syllabusService.deleteLesson(lessonId);
        redirectAttributes.addFlashAttribute("successMessage", "Lesson deleted.");
        return "redirect:/admin/courses/" + courseId + "/syllabus";
    }

    @PostMapping("/admin/lessons/{lessonId}/move")
    public String moveLesson(@PathVariable Long lessonId, @RequestParam("direction") String direction) {
        Long courseId = syllabusService.moveLesson(lessonId, "down".equalsIgnoreCase(direction) ? 1 : -1);
        return "redirect:/admin/courses/" + courseId + "/syllabus";
    }

    @PostMapping("/admin/lessons/{lessonId}/publish")
    public String publishLesson(@PathVariable Long lessonId, RedirectAttributes redirectAttributes) {
        LessonDto lesson = syllabusService.setLessonPublished(lessonId, true);
        redirectAttributes.addFlashAttribute("successMessage", "Lesson published — visible to enrolled students.");
        return "redirect:/admin/courses/" + lesson.getCourseId() + "/syllabus";
    }

    @PostMapping("/admin/lessons/{lessonId}/unpublish")
    public String unpublishLesson(@PathVariable Long lessonId, RedirectAttributes redirectAttributes) {
        LessonDto lesson = syllabusService.setLessonPublished(lessonId, false);
        redirectAttributes.addFlashAttribute("successMessage", "Lesson unpublished — hidden from students.");
        return "redirect:/admin/courses/" + lesson.getCourseId() + "/syllabus";
    }

    @PostMapping("/admin/lessons/{lessonId}/resources")
    public String addResource(@PathVariable Long lessonId,
                              @Valid @ModelAttribute("resourceDto") LessonResourceDto resourceDto,
                              BindingResult bindingResult,
                              @RequestParam(value = "file", required = false) MultipartFile file,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstFieldError(bindingResult));
            return "redirect:/admin/lessons/" + lessonId + "/edit";
        }
        try {
            syllabusService.addResource(lessonId, resourceDto, file);
            redirectAttributes.addFlashAttribute("successMessage", "Resource added.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/lessons/" + lessonId + "/edit";
    }

    @PostMapping("/admin/resources/{resourceId}/delete")
    public String deleteResource(@PathVariable Long resourceId, RedirectAttributes redirectAttributes) {
        Long lessonId = syllabusService.deleteResource(resourceId);
        redirectAttributes.addFlashAttribute("successMessage", "Resource deleted.");
        return "redirect:/admin/lessons/" + lessonId + "/edit";
    }

    private void populateLessonForm(Model model, LessonDto lessonDto, String formAction, boolean editing, CourseModuleDto module) {
        model.addAttribute("pageTitle", (editing ? "Edit Lesson" : "New Lesson") + " — Admin");
        model.addAttribute("lessonDto", lessonDto);
        model.addAttribute("formAction", formAction);
        model.addAttribute("editing", editing);
        model.addAttribute("module", module);
        model.addAttribute("lessonTypes", LessonType.values());
        model.addAttribute("resourceTypes", syllabusService.getResourceTypes());
    }

    private String firstFieldError(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null
                        ? "Please review the highlighted form fields."
                        : error.getDefaultMessage())
                .orElse("Please review the highlighted form fields.");
    }
}
