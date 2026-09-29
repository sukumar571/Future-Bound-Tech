package com.futureboundtech.service.impl;

import com.futureboundtech.dto.AnnouncementDto;
import com.futureboundtech.dto.ContactFormDto;
import com.futureboundtech.dto.FaqDto;
import com.futureboundtech.dto.InstituteSettingsDto;
import com.futureboundtech.dto.ReviewDto;
import com.futureboundtech.dto.TrainerDto;
import com.futureboundtech.entity.Announcement;
import com.futureboundtech.entity.ContactMessage;
import com.futureboundtech.entity.Course;
import com.futureboundtech.entity.FaqItem;
import com.futureboundtech.entity.InstituteSettings;
import com.futureboundtech.entity.Review;
import com.futureboundtech.entity.Trainer;
import com.futureboundtech.entity.User;
import com.futureboundtech.enums.ContactStatus;
import com.futureboundtech.enums.CourseStatus;
import com.futureboundtech.repository.AnnouncementRepository;
import com.futureboundtech.repository.ContactMessageRepository;
import com.futureboundtech.repository.FaqItemRepository;
import com.futureboundtech.repository.InstituteSettingsRepository;
import com.futureboundtech.repository.ReviewRepository;
import com.futureboundtech.repository.TrainerRepository;
import com.futureboundtech.service.WebsiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Public-facing website reads plus the contact-form write. Every read is
 * limited to content the institute has chosen to publish.
 */
@Service
@RequiredArgsConstructor
public class WebsiteServiceImpl implements WebsiteService {

    private final InstituteSettingsRepository instituteSettingsRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final FaqItemRepository faqItemRepository;
    private final ReviewRepository reviewRepository;
    private final TrainerRepository trainerRepository;
    private final AnnouncementRepository announcementRepository;

    /** Env-backed Razorpay flag surfaced to the UI as a read-only payment indicator. */
    @Value("${razorpay.enabled:false}")
    private boolean razorpayEnabled;

    @Override
    @Transactional(readOnly = true)
    public InstituteSettingsDto publicSettings() {
        InstituteSettings settings = instituteSettingsRepository.findAll().stream().findFirst().orElse(null);
        InstituteSettingsDto dto = new InstituteSettingsDto();
        if (settings == null) {
            dto.setName("Future Bound Tech");
            dto.setPhone1("8978866005");
            dto.setPhone2("7893702635");
            return dto;
        }
        dto.setId(settings.getId())
                .setName(settings.getName())
                .setTagLine(settings.getTagLine())
                .setPhone1(StringUtils.hasText(settings.getPhone1()) ? settings.getPhone1() : "8978866005")
                .setPhone2(settings.getPhone2())
                .setEmail(settings.getEmail())
                .setWebsite(settings.getWebsite())
                .setAddress(settings.getAddress())
                .setFacebookUrl(settings.getFacebookUrl())
                .setTwitterUrl(settings.getTwitterUrl())
                .setInstagramUrl(settings.getInstagramUrl())
                .setLinkedinUrl(settings.getLinkedinUrl())
                .setYoutubeUrl(settings.getYoutubeUrl());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public InstituteSettingsDto siteSettings() {
        InstituteSettings settings = instituteSettingsRepository.findAll().stream().findFirst().orElse(null);
        InstituteSettingsDto dto = new InstituteSettingsDto();
        dto.setPaymentEnabled(razorpayEnabled);
        if (settings == null) {
            return applyDefaults(dto, null);
        }
        dto.setId(settings.getId())
                .setName(settings.getName())
                .setTagLine(settings.getTagLine())
                .setPhone1(settings.getPhone1())
                .setPhone2(settings.getPhone2())
                .setEmail(settings.getEmail())
                .setWebsite(settings.getWebsite())
                .setAddress(settings.getAddress())
                .setTrainingModes(settings.getTrainingModes())
                .setLogoUrl(resolvePublicUrl(settings.getLogoUrl()))
                .setFaviconUrl(resolvePublicUrl(settings.getFaviconUrl()))
                .setPrimaryColor(settings.getPrimaryColor())
                .setSecondaryColor(settings.getSecondaryColor())
                .setAccentColor(settings.getAccentColor())
                .setFacebookUrl(settings.getFacebookUrl())
                .setTwitterUrl(settings.getTwitterUrl())
                .setInstagramUrl(settings.getInstagramUrl())
                .setLinkedinUrl(settings.getLinkedinUrl())
                .setYoutubeUrl(settings.getYoutubeUrl())
                .setDefaultRegistrationFee(settings.getDefaultRegistrationFee())
                .setCurrency(settings.getCurrency())
                .setDefaultDurationMonths(settings.getDefaultDurationMonths())
                .setDefaultLevel(settings.getDefaultLevel())
                .setDefaultTrainingMode(settings.getDefaultTrainingMode())
                .setDefaultCertificateEligible(settings.isDefaultCertificateEligible())
                .setDefaultPublicListed(settings.isDefaultPublicListed())
                .setPaymentGatewayLabel(settings.getPaymentGatewayLabel())
                .setPaymentInstructions(settings.getPaymentInstructions())
                .setAllowCoupons(settings.isAllowCoupons())
                .setEmailNotifications(settings.isEmailNotifications())
                .setWhatsappNotifications(settings.isWhatsappNotifications())
                .setSmsNotifications(settings.isSmsNotifications())
                .setCertificateSignatory(settings.getCertificateSignatory())
                .setCertificateSignatoryTitle(settings.getCertificateSignatoryTitle())
                .setCertificateFooterNote(settings.getCertificateFooterNote())
                .setPrivacyPolicy(settings.getPrivacyPolicy())
                .setTermsAndConditions(settings.getTermsAndConditions());
        return applyDefaults(dto, settings);
    }

    /** Fills spec defaults / "Not decided" fallbacks so templates need no null checks. */
    private InstituteSettingsDto applyDefaults(InstituteSettingsDto dto, InstituteSettings settings) {
        if (!StringUtils.hasText(dto.getName())) {
            dto.setName("Future Bound Tech");
        }
        if (!StringUtils.hasText(dto.getTagLine())) {
            dto.setTagLine("Learn Today. Build Tomorrow.");
        }
        if (!StringUtils.hasText(dto.getPhone1())) {
            dto.setPhone1("8978866005");
        }
        if (!StringUtils.hasText(dto.getPhone2())) {
            dto.setPhone2("7893702635");
        }
        if (!StringUtils.hasText(dto.getEmail())) {
            dto.setEmail("Not decided");
        }
        if (!StringUtils.hasText(dto.getAddress())) {
            dto.setAddress("Not decided");
        }
        if (!StringUtils.hasText(dto.getTrainingModes())) {
            dto.setTrainingModes("Online + Offline");
        }
        if (!StringUtils.hasText(dto.getPaymentGatewayLabel())) {
            dto.setPaymentGatewayLabel("Razorpay");
        }
        return dto;
    }

    @Override
    @Transactional
    public void submitContact(ContactFormDto form) {
        String interest = trimToNull(form.getCourseInterest());
        ContactMessage message = ContactMessage.builder()
                .name(form.getName().trim())
                .email(form.getEmail().trim())
                .phone(trimToNull(form.getPhone()))
                .courseInterest(interest)
                .subject(interest != null ? "Course enquiry: " + interest : "General enquiry")
                .message(form.getMessage().trim())
                .status(ContactStatus.NEW)
                .isReplied(false)
                .build();
        contactMessageRepository.save(message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FaqDto> publishedFaqs() {
        return faqItemRepository.findByPublishedTrueOrderBySortOrderAscCreatedAtAsc().stream()
                .map(this::toFaqDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewDto> publishedTestimonials() {
        return reviewRepository.findByPublishedTrueOrderByCreatedAtDesc().stream()
                .map(this::toReviewDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainerDto> publicTrainers() {
        return trainerRepository.findAllWithUser().stream()
                .filter(t -> t.getUser() != null && t.getUser().isActive())
                .map(this::toTrainerDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TrainerDto publicTrainer(Long id) {
        return trainerRepository.findById(id)
                .filter(t -> t.getUser() != null && t.getUser().isActive())
                .map(this::toTrainerDto)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> publicTrainerCourseTitles(Long id) {
        return trainerRepository.findById(id).map(trainer -> publicCourses(trainer).stream()
                .map(Course::getTitle)
                .collect(Collectors.toList())).orElseGet(List::of);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementDto> publicAnnouncements() {
        return announcementRepository.findByBatchIsNullOrderByCreatedAtDesc().stream()
                .map(this::toAnnouncementDto)
                .collect(Collectors.toList());
    }

    // ================= Mappers =================

    private FaqDto toFaqDto(FaqItem item) {
        return new FaqDto()
                .setId(item.getId())
                .setQuestion(item.getQuestion())
                .setAnswer(item.getAnswer())
                .setCategory(item.getCategory())
                .setPublished(item.isPublished())
                .setSortOrder(item.getSortOrder())
                .setCreatedAt(item.getCreatedAt());
    }

    private ReviewDto toReviewDto(Review review) {
        String studentName = review.getStudent() != null && review.getStudent().getUser() != null
                ? fullName(review.getStudent().getUser()) : null;
        String courseTitle = review.getCourse() != null ? review.getCourse().getTitle() : null;
        String author = StringUtils.hasText(review.getAuthorName()) ? review.getAuthorName() : studentName;
        String context = StringUtils.hasText(review.getContextLabel()) ? review.getContextLabel() : courseTitle;
        return new ReviewDto()
                .setId(review.getId())
                .setStudentName(studentName)
                .setCourseTitle(courseTitle)
                .setAuthorName(author)
                .setContextLabel(context)
                .setRating(review.getRating())
                .setComment(review.getComment())
                .setApproved(review.isApproved())
                .setDemo(review.isDemo())
                .setPublished(review.isPublished())
                .setStatusLabel(review.isPublished() ? "Published" : (review.isApproved() ? "Approved" : "Pending"))
                .setCreatedAt(review.getCreatedAt());
    }

    private TrainerDto toTrainerDto(Trainer trainer) {
        User user = trainer.getUser();
        List<Course> courses = publicCourses(trainer);
        return new TrainerDto()
                .setId(trainer.getId())
                .setUserId(user.getId())
                .setFirstName(user.getFirstName())
                .setLastName(user.getLastName())
                .setFullName(fullName(user))
                .setEmail(user.getEmail())
                .setPhone(user.getPhone())
                .setExpertise(trainer.getExpertise())
                .setBio(trainer.getBio())
                .setExperienceYears(trainer.getExperienceYears())
                .setPhotoUrl(resolvePublicUrl(trainer.getPhotoUrl()))
                .setActive(user.isActive())
                .setCourseCount(courses.size())
                .setBatchCount(trainer.getBatches() == null ? 0 : trainer.getBatches().size())
                .setCreatedAt(trainer.getCreatedAt());
    }

    private AnnouncementDto toAnnouncementDto(Announcement announcement) {
        return new AnnouncementDto()
                .setId(announcement.getId())
                .setTitle(announcement.getTitle())
                .setContent(announcement.getContent())
                .setGlobal(true)
                .setBatchName("All students")
                .setCreatedAt(announcement.getCreatedAt());
    }

    // ================= Helpers =================

    private List<Course> publicCourses(Trainer trainer) {
        if (trainer.getCourses() == null) {
            return List.of();
        }
        return trainer.getCourses().stream()
                .filter(c -> !c.isDeleted())
                .filter(c -> c.getStatus() == CourseStatus.PUBLISHED)
                .collect(Collectors.toList());
    }

    /** A stored photo path is served from /uploads; an absolute URL is left untouched. */
    private String resolvePublicUrl(String stored) {
        if (!StringUtils.hasText(stored)) {
            return null;
        }
        String value = stored.trim();
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("/")) {
            return value;
        }
        return "/uploads/" + value.replace("\\", "/");
    }

    private String fullName(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName();
        String last = user.getLastName() == null ? "" : user.getLastName();
        return (first + " " + last).trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
