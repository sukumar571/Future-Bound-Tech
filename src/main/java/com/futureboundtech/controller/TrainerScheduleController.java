package com.futureboundtech.controller;

import com.futureboundtech.dto.AnnouncementDto;
import com.futureboundtech.dto.TrainerClassFormDto;
import com.futureboundtech.enums.ClassMode;
import com.futureboundtech.enums.ClassStatus;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.security.CustomUserDetails;
import com.futureboundtech.service.TrainerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
@RequestMapping("/trainer")
public class TrainerScheduleController {

    private final TrainerService trainerService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, true));
        binder.registerCustomEditor(Integer.class, new CustomNumberEditor(Integer.class, true));
        binder.registerCustomEditor(ClassMode.class, new EnumPropertyEditor<>(ClassMode.class));
        binder.registerCustomEditor(ClassStatus.class, new EnumPropertyEditor<>(ClassStatus.class));
    }

    /** Lenient enum binder so a typo in hidden form fields cannot 500 the page. */
    private static final class EnumPropertyEditor<T extends Enum<T>> extends java.beans.PropertyEditorSupport {
        private final Class<T> enumType;

        private EnumPropertyEditor(Class<T> enumType) {
            this.enumType = enumType;
        }

        @Override
        public void setAsText(String text) {
            if (text == null || text.isBlank()) {
                setValue(null);
                return;
            }
            try {
                setValue(Enum.valueOf(enumType, text.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                setValue(null);
            }
        }
    }

    // ==================== Classes ====================

    @GetMapping("/classes")
    public String classes(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pageTitle", "My Classes — Trainer");
        model.addAttribute("classes", trainerService.listClasses(principal.getUser()));
        return "trainer/classes";
    }

    @GetMapping("/classes/new")
    public String newClass(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        populateClassForm(model, new TrainerClassFormDto(), principal, "/trainer/classes/new", false, null);
        return "trainer/class-form";
    }

    @PostMapping("/classes/new")
    public String createClass(@AuthenticationPrincipal CustomUserDetails principal,
                              @Valid @ModelAttribute("classForm") TrainerClassFormDto form,
                              BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            populateClassForm(model, form, principal, "/trainer/classes/new", false, null);
            return "trainer/class-form";
        }
        try {
            trainerService.createClass(principal.getUser(), form);
        } catch (BusinessException ex) {
            populateClassForm(model, form, principal, "/trainer/classes/new", false, null);
            model.addAttribute("errorMessage", ex.getMessage());
            return "trainer/class-form";
        }
        return "redirect:/trainer/classes";
    }

    @GetMapping("/classes/{id}/edit")
    public String editClass(@AuthenticationPrincipal CustomUserDetails principal,
                            @PathVariable Long id, Model model) {
        populateClassForm(model, trainerService.getClass(principal.getUser(), id), principal,
                "/trainer/classes/" + id + "/edit", true, id);
        return "trainer/class-form";
    }

    @PostMapping("/classes/{id}/edit")
    public String updateClass(@AuthenticationPrincipal CustomUserDetails principal,
                              @PathVariable Long id,
                              @Valid @ModelAttribute("classForm") TrainerClassFormDto form,
                              BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            populateClassForm(model, form, principal, "/trainer/classes/" + id + "/edit", true, id);
            return "trainer/class-form";
        }
        try {
            trainerService.updateClass(principal.getUser(), id, form);
        } catch (BusinessException ex) {
            populateClassForm(model, form, principal, "/trainer/classes/" + id + "/edit", true, id);
            model.addAttribute("errorMessage", ex.getMessage());
            return "trainer/class-form";
        }
        return "redirect:/trainer/classes";
    }

    @PostMapping("/classes/{id}/delete")
    public String deleteClass(@AuthenticationPrincipal CustomUserDetails principal,
                              @PathVariable Long id, RedirectAttributes ra) {
        trainerService.deleteClass(principal.getUser(), id);
        ra.addFlashAttribute("successMessage", "Class deleted.");
        return "redirect:/trainer/classes";
    }

    @GetMapping("/classes/{id}/attendance")
    public String attendance(@AuthenticationPrincipal CustomUserDetails principal,
                             @PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "Mark Attendance — Trainer");
        model.addAttribute("roster", trainerService.attendanceRoster(principal.getUser(), id));
        return "trainer/attendance";
    }

    @PostMapping("/classes/{id}/attendance")
    public String markAttendance(@AuthenticationPrincipal CustomUserDetails principal,
                                 @PathVariable Long id,
                                 @RequestParam(name = "presentStudentId", required = false) List<Long> presentStudentIds,
                                 @RequestParam(required = false) String status,
                                 RedirectAttributes ra) {
        trainerService.markAttendance(principal.getUser(), id, presentStudentIds, status);
        ra.addFlashAttribute("successMessage", "Attendance saved.");
        return "redirect:/trainer/classes/" + id + "/attendance";
    }

    // ==================== Announcements ====================

    @GetMapping("/announcements")
    public String announcements(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pageTitle", "Announcements — Trainer");
        model.addAttribute("announcements", trainerService.listAnnouncements(principal.getUser()));
        return "trainer/announcements";
    }

    @GetMapping("/announcements/new")
    public String newAnnouncement(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        populateAnnouncementForm(model, new AnnouncementDto(), principal, "/trainer/announcements/new");
        return "trainer/announcement-form";
    }

    @PostMapping("/announcements/new")
    public String createAnnouncement(@AuthenticationPrincipal CustomUserDetails principal,
                                     @Valid @ModelAttribute("announcementForm") AnnouncementDto dto,
                                     BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            populateAnnouncementForm(model, dto, principal, "/trainer/announcements/new");
            return "trainer/announcement-form";
        }
        trainerService.createAnnouncement(principal.getUser(), dto);
        return "redirect:/trainer/announcements";
    }

    @PostMapping("/announcements/{id}/delete")
    public String deleteAnnouncement(@AuthenticationPrincipal CustomUserDetails principal,
                                     @PathVariable Long id, RedirectAttributes ra) {
        trainerService.deleteAnnouncement(principal.getUser(), id);
        ra.addFlashAttribute("successMessage", "Announcement deleted.");
        return "redirect:/trainer/announcements";
    }

    // ==================== Helpers ====================

    private void populateClassForm(Model model, TrainerClassFormDto form,
                                   CustomUserDetails principal, String action, boolean editing, Long id) {
        model.addAttribute("pageTitle", (editing ? "Edit Class" : "Schedule Class") + " — Trainer");
        model.addAttribute("classForm", form);
        model.addAttribute("formAction", action);
        model.addAttribute("editing", editing);
        model.addAttribute("classId", id);
        model.addAttribute("batches", trainerService.listBatches(principal.getUser()));
        model.addAttribute("modes", ClassMode.values());
        model.addAttribute("statuses", ClassStatus.values());
    }

    private void populateAnnouncementForm(Model model, AnnouncementDto dto,
                                          CustomUserDetails principal, String action) {
        model.addAttribute("pageTitle", "New Announcement — Trainer");
        model.addAttribute("announcementForm", dto);
        model.addAttribute("formAction", action);
        model.addAttribute("batches", trainerService.listBatches(principal.getUser()));
    }
}
