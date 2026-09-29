package com.futureboundtech.controller;

import com.futureboundtech.dto.AnnouncementDto;
import com.futureboundtech.dto.FaqDto;
import com.futureboundtech.dto.ReviewDto;
import com.futureboundtech.enums.ContactStatus;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.service.AdminService;
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
public class AdminContentController {

    private final AdminService adminService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, true));
    }

    // -----------------------------------------------------------------
    // Announcements
    // -----------------------------------------------------------------
    @GetMapping("/announcements")
    public String announcements(Model model) {
        model.addAttribute("pageTitle", "Announcements — Admin");
        model.addAttribute("announcements", adminService.listAnnouncements());
        return "admin/announcements";
    }

    @GetMapping("/announcements/new")
    public String newAnnouncementForm(Model model) {
        populateAnnouncementForm(model, new AnnouncementDto(), false, null);
        return "admin/announcement-form";
    }

    @PostMapping("/announcements")
    public String createAnnouncement(@Valid @ModelAttribute("announcementDto") AnnouncementDto dto,
                                     BindingResult bindingResult,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateAnnouncementForm(model, dto, false, null);
            model.addAttribute("errorMessage", "Please review the highlighted fields.");
            return "admin/announcement-form";
        }
        try {
            adminService.createAnnouncement(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Announcement published.");
            return "redirect:/admin/announcements";
        } catch (BusinessException ex) {
            populateAnnouncementForm(model, dto, false, null);
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/announcement-form";
        }
    }

    @GetMapping("/announcements/{id}/edit")
    public String editAnnouncementForm(@PathVariable Long id, Model model) {
        populateAnnouncementForm(model, adminService.getAnnouncement(id), true, id);
        return "admin/announcement-form";
    }

    @PostMapping("/announcements/{id}")
    public String updateAnnouncement(@PathVariable Long id,
                                     @Valid @ModelAttribute("announcementDto") AnnouncementDto dto,
                                     BindingResult bindingResult,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateAnnouncementForm(model, dto, true, id);
            model.addAttribute("errorMessage", "Please review the highlighted fields.");
            return "admin/announcement-form";
        }
        try {
            adminService.updateAnnouncement(id, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Announcement updated.");
            return "redirect:/admin/announcements";
        } catch (BusinessException ex) {
            populateAnnouncementForm(model, dto, true, id);
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/announcement-form";
        }
    }

    @PostMapping("/announcements/{id}/delete")
    public String deleteAnnouncement(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminService.deleteAnnouncement(id);
        redirectAttributes.addFlashAttribute("successMessage", "Announcement deleted.");
        return "redirect:/admin/announcements";
    }

    // -----------------------------------------------------------------
    // Contact messages
    // -----------------------------------------------------------------
    @GetMapping("/contact-messages")
    public String contactMessages(@RequestParam(value = "q", required = false) String q,
                                  @RequestParam(value = "status", required = false) String status,
                                  Model model) {
        ContactStatus parsed = parseStatus(status);
        model.addAttribute("pageTitle", "Contact Messages — Admin");
        model.addAttribute("messages", adminService.listContactMessages(q, parsed));
        model.addAttribute("statuses", ContactStatus.values());
        model.addAttribute("searchQuery", q);
        model.addAttribute("statusFilter", parsed == null ? "" : parsed.name());
        return "admin/contact-messages";
    }

    private ContactStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ContactStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @PostMapping("/contact-messages/{id}/replied")
    public String markReplied(@PathVariable Long id,
                              @RequestParam(value = "replied", defaultValue = "true") boolean replied,
                              RedirectAttributes redirectAttributes) {
        adminService.setContactReplied(id, replied);
        redirectAttributes.addFlashAttribute("successMessage", replied ? "Marked as replied." : "Marked as new.");
        return "redirect:/admin/contact-messages";
    }

    @PostMapping("/contact-messages/{id}/status")
    public String updateContactStatus(@PathVariable Long id,
                                      @RequestParam("status") String status,
                                      RedirectAttributes redirectAttributes) {
        ContactStatus parsed = parseStatus(status);
        if (parsed == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unknown status.");
            return "redirect:/admin/contact-messages";
        }
        adminService.setContactStatus(id, parsed);
        redirectAttributes.addFlashAttribute("successMessage", "Status updated to " + parsed.getLabel() + ".");
        return "redirect:/admin/contact-messages";
    }

    // -----------------------------------------------------------------
    // Testimonials (reviews)
    // -----------------------------------------------------------------
    @GetMapping("/testimonials")
    public String testimonials(Model model) {
        model.addAttribute("pageTitle", "Testimonials — Admin");
        model.addAttribute("reviews", adminService.listReviews());
        return "admin/testimonials";
    }

    @GetMapping("/testimonials/new")
    public String newTestimonialForm(Model model) {
        populateReviewForm(model, new ReviewDto(), false, null);
        return "admin/testimonial-form";
    }

    @PostMapping("/testimonials")
    public String createTestimonial(@Valid @ModelAttribute("reviewDto") ReviewDto dto,
                                    BindingResult bindingResult,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateReviewForm(model, dto, false, null);
            model.addAttribute("errorMessage", "Please review the highlighted fields.");
            return "admin/testimonial-form";
        }
        adminService.createReview(dto);
        redirectAttributes.addFlashAttribute("successMessage", "Testimonial created.");
        return "redirect:/admin/testimonials";
    }

    @GetMapping("/testimonials/{id}/edit")
    public String editTestimonialForm(@PathVariable Long id, Model model) {
        populateReviewForm(model, adminService.getReview(id), true, id);
        return "admin/testimonial-form";
    }

    @PostMapping("/testimonials/{id}")
    public String updateTestimonial(@PathVariable Long id,
                                    @Valid @ModelAttribute("reviewDto") ReviewDto dto,
                                    BindingResult bindingResult,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateReviewForm(model, dto, true, id);
            model.addAttribute("errorMessage", "Please review the highlighted fields.");
            return "admin/testimonial-form";
        }
        adminService.updateReview(id, dto);
        redirectAttributes.addFlashAttribute("successMessage", "Testimonial updated.");
        return "redirect:/admin/testimonials";
    }

    @PostMapping("/testimonials/{id}/delete")
    public String deleteTestimonial(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminService.deleteReview(id);
        redirectAttributes.addFlashAttribute("successMessage", "Testimonial deleted.");
        return "redirect:/admin/testimonials";
    }

    @PostMapping("/testimonials/{id}/approve")
    public String approveReview(@PathVariable Long id,
                                @RequestParam("approved") boolean approved,
                                RedirectAttributes redirectAttributes) {
        adminService.setReviewApproved(id, approved);
        redirectAttributes.addFlashAttribute("successMessage", approved ? "Testimonial approved." : "Testimonial approval revoked.");
        return "redirect:/admin/testimonials";
    }

    @PostMapping("/testimonials/{id}/publish")
    public String publishReview(@PathVariable Long id,
                                @RequestParam("published") boolean published,
                                RedirectAttributes redirectAttributes) {
        adminService.setReviewPublished(id, published);
        redirectAttributes.addFlashAttribute("successMessage", published ? "Testimonial is now public." : "Testimonial hidden from the site.");
        return "redirect:/admin/testimonials";
    }

    // -----------------------------------------------------------------
    // FAQs
    // -----------------------------------------------------------------
    @GetMapping("/faqs")
    public String faqs(Model model) {
        model.addAttribute("pageTitle", "FAQs — Admin");
        model.addAttribute("faqs", adminService.listFaqs());
        return "admin/faq-list";
    }

    @GetMapping("/faqs/new")
    public String newFaqForm(Model model) {
        populateFaqForm(model, new FaqDto(), false, null);
        return "admin/faq-form";
    }

    @PostMapping("/faqs")
    public String createFaq(@Valid @ModelAttribute("faqDto") FaqDto dto,
                            BindingResult bindingResult,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFaqForm(model, dto, false, null);
            model.addAttribute("errorMessage", "Please review the highlighted fields.");
            return "admin/faq-form";
        }
        adminService.createFaq(dto);
        redirectAttributes.addFlashAttribute("successMessage", "FAQ created.");
        return "redirect:/admin/faqs";
    }

    @GetMapping("/faqs/{id}/edit")
    public String editFaqForm(@PathVariable Long id, Model model) {
        populateFaqForm(model, adminService.getFaq(id), true, id);
        return "admin/faq-form";
    }

    @PostMapping("/faqs/{id}")
    public String updateFaq(@PathVariable Long id,
                            @Valid @ModelAttribute("faqDto") FaqDto dto,
                            BindingResult bindingResult,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFaqForm(model, dto, true, id);
            model.addAttribute("errorMessage", "Please review the highlighted fields.");
            return "admin/faq-form";
        }
        adminService.updateFaq(id, dto);
        redirectAttributes.addFlashAttribute("successMessage", "FAQ updated.");
        return "redirect:/admin/faqs";
    }

    @PostMapping("/faqs/{id}/delete")
    public String deleteFaq(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminService.deleteFaq(id);
        redirectAttributes.addFlashAttribute("successMessage", "FAQ deleted.");
        return "redirect:/admin/faqs";
    }

    @PostMapping("/faqs/{id}/publish")
    public String publishFaq(@PathVariable Long id,
                             @RequestParam("published") boolean published,
                             RedirectAttributes redirectAttributes) {
        adminService.setFaqPublished(id, published);
        redirectAttributes.addFlashAttribute("successMessage", published ? "FAQ published." : "FAQ unpublished.");
        return "redirect:/admin/faqs";
    }

    // -----------------------------------------------------------------
    // Assignments & quizzes (read only)
    // -----------------------------------------------------------------
    @GetMapping("/assignments")
    public String assignments(Model model) {
        model.addAttribute("pageTitle", "Assignments — Admin");
        model.addAttribute("assignments", adminService.listAssignments());
        return "admin/assignments";
    }

    @GetMapping("/quizzes")
    public String quizzes(Model model) {
        model.addAttribute("pageTitle", "Quizzes — Admin");
        model.addAttribute("quizzes", adminService.listQuizzes());
        return "admin/quizzes";
    }

    // -----------------------------------------------------------------
    // Placement eligibility (certified students; posts live in AdminPlacementController)
    // -----------------------------------------------------------------
    @GetMapping("/placements/eligibility")
    public String placements(Model model) {
        model.addAttribute("pageTitle", "Placement Eligibility — Admin");
        model.addAttribute("certificates", adminService.listCertificates());
        return "admin/placement-eligibility";
    }

    private void populateAnnouncementForm(Model model, AnnouncementDto dto, boolean editing, Long id) {
        model.addAttribute("pageTitle", (editing ? "Edit Announcement" : "New Announcement") + " — Admin");
        model.addAttribute("announcementDto", dto);
        model.addAttribute("editing", editing);
        model.addAttribute("announcementId", id);
        model.addAttribute("formAction", editing ? "/admin/announcements/" + id : "/admin/announcements");
        model.addAttribute("batches", adminService.listBatches());
    }

    private void populateReviewForm(Model model, ReviewDto dto, boolean editing, Long id) {
        model.addAttribute("pageTitle", (editing ? "Edit Testimonial" : "New Testimonial") + " — Admin");
        model.addAttribute("reviewDto", dto);
        model.addAttribute("editing", editing);
        model.addAttribute("reviewId", id);
        model.addAttribute("formAction", editing ? "/admin/testimonials/" + id : "/admin/testimonials");
    }

    private void populateFaqForm(Model model, FaqDto dto, boolean editing, Long id) {
        model.addAttribute("pageTitle", (editing ? "Edit FAQ" : "New FAQ") + " — Admin");
        model.addAttribute("faqDto", dto);
        model.addAttribute("editing", editing);
        model.addAttribute("faqId", id);
        model.addAttribute("formAction", editing ? "/admin/faqs/" + id : "/admin/faqs");
    }
}
