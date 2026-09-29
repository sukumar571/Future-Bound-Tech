package com.futureboundtech.controller;

import com.futureboundtech.dto.TrainerProfileFormDto;
import com.futureboundtech.security.CustomUserDetails;
import com.futureboundtech.service.CourseService;
import com.futureboundtech.service.TrainerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/trainer")
public class TrainerController {

    private final TrainerService trainerService;
    private final CourseService courseService;

    @GetMapping({"", "/dashboard"})
    public String dashboard(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        if (principal != null && principal.getUser().getRole().name().equals("ADMIN")) {
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("pageTitle", "Trainer Dashboard");
        model.addAttribute("stats", trainerService.dashboard(principal.getUser()));
        return "trainer/dashboard";
    }

    @GetMapping("/courses")
    public String courses(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pageTitle", "My Courses — Trainer");
        model.addAttribute("courses", courseService.findCoursesForTrainer(principal.getUser()));
        Map<Long, Integer> batchCounts = new HashMap<>();
        trainerService.listBatches(principal.getUser())
                .forEach(b -> batchCounts.merge(b.getCourseId(), 1, Integer::sum));
        model.addAttribute("batchCounts", batchCounts);
        return "trainer/courses";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pageTitle", "My Profile — Trainer");
        model.addAttribute("profileForm", trainerService.getProfile(principal.getUser()));
        return "trainer/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails principal,
                                @Valid @ModelAttribute("profileForm") TrainerProfileFormDto form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "My Profile — Trainer");
            return "trainer/profile";
        }
        trainerService.updateProfile(principal.getUser(), form);
        ra.addFlashAttribute("successMessage", "Profile updated.");
        return "redirect:/trainer/profile";
    }

    @GetMapping("/batches")
    public String batches(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pageTitle", "My Batches — Trainer");
        model.addAttribute("batches", trainerService.listBatches(principal.getUser()));
        return "trainer/batches";
    }

    @GetMapping("/students")
    public String students(@AuthenticationPrincipal CustomUserDetails principal,
                           @RequestParam(required = false) Long batchId,
                           @RequestParam(required = false) String q,
                           Model model) {
        model.addAttribute("pageTitle", "My Students — Trainer");
        model.addAttribute("students", trainerService.listStudents(principal.getUser(), batchId, q));
        model.addAttribute("batches", trainerService.listBatches(principal.getUser()));
        model.addAttribute("selectedBatchId", batchId);
        model.addAttribute("q", q);
        return "trainer/students";
    }

    @GetMapping("/students/{id}")
    public String studentDetail(@AuthenticationPrincipal CustomUserDetails principal,
                                @PathVariable Long id,
                                Model model) {
        model.addAttribute("pageTitle", "Student — Trainer");
        model.addAttribute("student", trainerService.getStudent(principal.getUser(), id));
        model.addAttribute("progress", trainerService.studentProgress(principal.getUser(), id));
        return "trainer/student-detail";
    }
}
