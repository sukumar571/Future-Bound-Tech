package com.futureboundtech.controller;

import com.futureboundtech.dto.ContactFormDto;
import com.futureboundtech.dto.InstituteSettingsDto;
import com.futureboundtech.exception.ResourceNotFoundException;
import com.futureboundtech.security.CustomUserDetails;
import com.futureboundtech.service.CertificateService;
import com.futureboundtech.service.CourseService;
import com.futureboundtech.service.PlacementService;
import com.futureboundtech.service.PracticeService;
import com.futureboundtech.service.WebsiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PublicController {

    private final CourseService courseService;
    private final PlacementService placementService;
    private final CertificateService certificateService;
    private final WebsiteService websiteService;
    private final PracticeService practiceService;

    @GetMapping({ "/", "/home" })
    public String home(Model model) {
        model.addAttribute("pageTitle", "Future Bound Tech — Learn Today. Build Tomorrow.");
        model.addAttribute("featuredCourses", courseService.findFeaturedPublic(5));
        model.addAttribute("batches", courseService.findPublicEnrollableBatches().stream().limit(4).toList());
        model.addAttribute("testimonials", websiteService.publishedTestimonials().stream().limit(3).toList());
        return "public/home";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("pageTitle", "About Us — Future Bound Tech");
        return "public/about";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("pageTitle", "Contact Us — Future Bound Tech");
        model.addAttribute("contactForm", new ContactFormDto());
        model.addAttribute("settings", websiteService.siteSettings());
        model.addAttribute("courseInterests", courseInterests());
        return "public/contact";
    }

    @PostMapping("/contact")
    public String submitContact(@Valid @ModelAttribute("contactForm") ContactFormDto form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Contact Us — Future Bound Tech");
            model.addAttribute("settings", websiteService.siteSettings());
            model.addAttribute("courseInterests", courseInterests());
            return "public/contact";
        }
        websiteService.submitContact(form);
        redirectAttributes.addFlashAttribute("successMessage",
                "Thanks for reaching out — we'll get back to you shortly.");
        return "redirect:/contact";
    }

    @GetMapping("/faq")
    public String faq(Model model) {
        model.addAttribute("pageTitle", "FAQ — Future Bound Tech");
        model.addAttribute("faqs", websiteService.publishedFaqs());
        return "public/faq";
    }

    @GetMapping("/testimonials")
    public String testimonials(Model model) {
        model.addAttribute("pageTitle", "Testimonials — Future Bound Tech");
        model.addAttribute("testimonials", websiteService.publishedTestimonials());
        return "public/testimonials";
    }

    @GetMapping("/trainers")
    public String trainers(Model model) {
        model.addAttribute("pageTitle", "Our Trainers — Future Bound Tech");
        model.addAttribute("trainers", websiteService.publicTrainers());
        return "public/trainers";
    }

    @GetMapping("/trainers/{id}")
    public String trainerProfile(@PathVariable Long id, Model model) {
        var trainer = websiteService.publicTrainer(id);
        if (trainer == null) {
            throw new ResourceNotFoundException("Trainer not found.");
        }
        model.addAttribute("pageTitle", trainer.getFullName() + " — Future Bound Tech");
        model.addAttribute("trainer", trainer);
        model.addAttribute("courseTitles", websiteService.publicTrainerCourseTitles(id));
        return "public/trainer-profile";
    }

    private List<String> courseInterests() {
        return courseService.findFeaturedPublic(50).stream()
                .map(course -> course.getTitle())
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    @GetMapping("/placements")
    public String placements(@RequestParam(required = false) String skill, Model model) {
        model.addAttribute("pageTitle", "Placement Preparation — Future Bound Tech");
        model.addAttribute("opportunities", placementService.listPublished(skill).stream().limit(6).toList());
        model.addAttribute("skills", placementService.listDistinctSkills());
        model.addAttribute("fSkill", skill);
        return "public/placements";
    }

    @GetMapping("/community")
    public String community(Model model) {
        model.addAttribute("pageTitle", "Community — Future Bound Tech");
        model.addAttribute("announcements", websiteService.publicAnnouncements());
        model.addAttribute("faqs", websiteService.publishedFaqs());
        return "public/community";
    }

    // -----------------------------------------------------------------
    // Public certificate verification
    // -----------------------------------------------------------------
    @GetMapping("/certificate/verify")
    public String certificateVerify(@RequestParam(required = false) String number, Model model) {
        return renderVerify(model, number);
    }

    @GetMapping("/certificate/verify/{number}")
    public String certificateVerifyById(@PathVariable String number, Model model) {
        return renderVerify(model, number);
    }

    private String renderVerify(Model model, String number) {
        model.addAttribute("pageTitle", "Verify Certificate — Future Bound Tech");
        model.addAttribute("institutionName", certificateService.instituteName());
        model.addAttribute("number", number);
        if (StringUtils.hasText(number)) {
            model.addAttribute("result", certificateService.verify(number.trim()));
        }
        return "public/certificate-verify";
    }

    @GetMapping("/practice")
    public String practice(Model model) {
        model.addAttribute("pageTitle", "Practice — Future Bound Tech");
        model.addAttribute("stats", practiceService.publicPracticeStats());
        model.addAttribute("categoryCounts", practiceService.mcqCategoryCounts());
        return "public/practice";
    }

    @GetMapping("/batches")
    public String batches(Model model) {
        model.addAttribute("pageTitle", "Upcoming Batches — Future Bound Tech");
        model.addAttribute("batches", courseService.findPublicEnrollableBatches());
        return "public/batches";
    }

    @GetMapping("/privacy-policy")
    public String privacyPolicy(Model model) {
        InstituteSettingsDto site = websiteService.siteSettings();
        model.addAttribute("pageTitle", "Privacy Policy — " + site.getName());
        model.addAttribute("legalTitle", "Privacy Policy");
        model.addAttribute("legalContent", site.getPrivacyPolicy());
        return "public/legal";
    }

    @GetMapping("/terms")
    public String terms(Model model) {
        InstituteSettingsDto site = websiteService.siteSettings();
        model.addAttribute("pageTitle", "Terms & Conditions — " + site.getName());
        model.addAttribute("legalTitle", "Terms & Conditions");
        model.addAttribute("legalContent", site.getTermsAndConditions());
        return "public/legal";
    }

    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("pageTitle", "Access Denied — Future Bound Tech");
        return "error/access-denied";
    }

   @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        return switch (principal.getUser().getRole()) {
            case ADMIN -> "redirect:/admin/dashboard";
            case TRAINER -> "redirect:/trainer/profile";
            case STUDENT -> "redirect:/student/profile";
            default -> "redirect:/";
        };
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        return switch (principal.getUser().getRole()) {
            case ADMIN -> "redirect:/admin/dashboard";
            case TRAINER -> "redirect:/trainer/dashboard";
            case STUDENT -> "redirect:/student/dashboard";
            default -> "redirect:/";
        };
    }
}
