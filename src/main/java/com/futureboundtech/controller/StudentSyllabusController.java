package com.futureboundtech.controller;

import com.futureboundtech.dto.LessonDto;
import com.futureboundtech.security.CustomUserDetails;
import com.futureboundtech.service.CourseService;
import com.futureboundtech.service.SyllabusService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class StudentSyllabusController {

    private final SyllabusService syllabusService;
    private final CourseService courseService;

    @GetMapping("/student/courses/{courseId}/syllabus")
    public String syllabus(@PathVariable Long courseId,
                           @AuthenticationPrincipal CustomUserDetails principal,
                           Model model) {
        model.addAttribute("pageTitle", "Syllabus — Future Bound Tech");
        model.addAttribute("course", courseService.findById(courseId));
        model.addAttribute("modules", syllabusService.getStudentSyllabus(courseId, principal.getUser()));
        model.addAttribute("progress", syllabusService.getCourseProgress(courseId, principal.getUser()));
        return "student/syllabus";
    }

    @GetMapping("/student/lessons/{lessonId}")
    public String lesson(@PathVariable Long lessonId,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         Model model) {
        LessonDto lesson = syllabusService.getStudentLesson(lessonId, principal.getUser());
        model.addAttribute("pageTitle", lesson.getTitle() + " — Lesson");
        model.addAttribute("lesson", lesson);
        return "student/lesson";
    }

    @PostMapping("/student/lessons/{lessonId}/complete")
    public String complete(@PathVariable Long lessonId,
                           @AuthenticationPrincipal CustomUserDetails principal,
                           RedirectAttributes redirectAttributes) {
        syllabusService.setLessonCompleted(lessonId, principal.getUser(), true);
        redirectAttributes.addFlashAttribute("successMessage", "Lesson marked as complete.");
        return "redirect:/student/lessons/" + lessonId;
    }

    @PostMapping("/student/lessons/{lessonId}/incomplete")
    public String incomplete(@PathVariable Long lessonId,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             RedirectAttributes redirectAttributes) {
        syllabusService.setLessonCompleted(lessonId, principal.getUser(), false);
        redirectAttributes.addFlashAttribute("infoMessage", "Lesson marked as incomplete.");
        return "redirect:/student/lessons/" + lessonId;
    }
}
