package com.futureboundtech.service;

import com.futureboundtech.dto.AnnouncementDto;
import com.futureboundtech.dto.ContactFormDto;
import com.futureboundtech.dto.FaqDto;
import com.futureboundtech.dto.InstituteSettingsDto;
import com.futureboundtech.dto.ReviewDto;
import com.futureboundtech.dto.TrainerDto;

import java.util.List;

/**
 * Public-website reads and the contact form. Kept separate from {@code AdminService}
 * so {@code PublicController} never reaches into admin-only operations.
 */
public interface WebsiteService {

    /** Institute name, phones, e-mail and address for the contact page. */
    InstituteSettingsDto publicSettings();

    /**
     * Fully-resolved settings for dynamic rendering everywhere (navbar, footer, head
     * favicon + theme, legal pages). Includes logo/favicon as /uploads URLs, brand
     * colours, training modes, and "Not decided" fallbacks for email/address/fees.
     */
    InstituteSettingsDto siteSettings();

    /** Persist a public contact submission. */
    void submitContact(ContactFormDto form);

    List<FaqDto> publishedFaqs();

    List<ReviewDto> publishedTestimonials();

    List<TrainerDto> publicTrainers();

    TrainerDto publicTrainer(Long id);

    /** Trainer's assigned, publicly-listed course titles (no invented data). */
    List<String> publicTrainerCourseTitles(Long id);

    /** Global (institute-wide) announcements for the community page. */
    List<AnnouncementDto> publicAnnouncements();
}
