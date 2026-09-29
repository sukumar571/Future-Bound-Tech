package com.futureboundtech;

import com.futureboundtech.entity.FaqItem;
import com.futureboundtech.entity.InstituteSettings;
import com.futureboundtech.entity.Review;
import com.futureboundtech.enums.CourseLevel;
import com.futureboundtech.enums.TrainingMode;
import com.futureboundtech.repository.FaqItemRepository;
import com.futureboundtech.repository.InstituteSettingsRepository;
import com.futureboundtech.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.math.BigDecimal;

/**
 * Seeds content for the supporting website modules (Contact / FAQ / Testimonials).
 * Runs after the main {@link DataSeeder} and is fully idempotent — it only fills
 * gaps when the corresponding tables are empty. No trainer qualifications or real
 * student names are invented; testimonials are clearly labelled as demo content.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seeders.enabled", havingValue = "true", matchIfMissing = true)
public class WebsiteDataSeeder {

    private final InstituteSettingsRepository instituteSettingsRepository;
    private final FaqItemRepository faqItemRepository;
    private final ReviewRepository reviewRepository;

    @Bean
    @Order(2)
    public CommandLineRunner initWebsiteData() {
        return args -> {
            seedSettings();
            seedFaqs();
            seedDemoTestimonials();
        };
    }

    private void seedSettings() {
        if (instituteSettingsRepository.count() > 0) {
            return;
        }
        instituteSettingsRepository.save(InstituteSettings.builder()
                .name("Future Bound Tech")
                .tagLine("Learn Today. Build Tomorrow.")
                .phone1("8978866005")
                .phone2("7893702635")
                .trainingModes("Online + Offline")
                .primaryColor("#06B6D4")
                .secondaryColor("#0B1020")
                .accentColor("#F472B6")
                .defaultRegistrationFee(BigDecimal.ZERO)
                .currency("INR")
                .defaultDurationMonths(3)
                .defaultLevel(CourseLevel.BEGINNER)
                .defaultTrainingMode(TrainingMode.HYBRID)
                .defaultCertificateEligible(true)
                .defaultPublicListed(false)
                .paymentGatewayLabel("Razorpay")
                .allowCoupons(true)
                .emailNotifications(true)
                .whatsappNotifications(false)
                .smsNotifications(false)
                .privacyPolicy(DEFAULT_PRIVACY)
                .termsAndConditions(DEFAULT_TERMS)
                .build());
    }

    private static final String DEFAULT_PRIVACY =
            "Future Bound Tech respects your privacy.\n\n"
            + "Information we collect: the details you provide when registering, enrolling in a course, "
            + "or sending a contact enquiry (name, email, phone and course interest).\n\n"
            + "How we use it: to deliver training, manage your progress and payments, and respond to your enquiries. "
            + "We do not sell your personal data.\n\n"
            + "Data security: access is restricted to authorised staff. Payment processing is handled by our "
            + "payment gateway; card details are never stored on our servers.\n\n"
            + "Your rights: you may request a copy, correction, or deletion of your personal data by contacting us.\n\n"
            + "This policy is a configurable default and may be updated by the institute at any time.";

    private static final String DEFAULT_TERMS =
            "Terms & Conditions\n\n"
            + "1. Enrollment: seats are confirmed on registration and payment as applicable.\n"
            + "2. Course content is provided for learning purposes; the institute may update schedules and materials.\n"
            + "3. Payments and refunds are governed by the fee policy communicated at the time of enrollment.\n"
            + "4. Certificates are issued on meeting the completion criteria and are verifiable through our portal.\n"
            + "5. Placement assistance (resume building, mock interviews, and profile sharing) is offered but "
            + "job placement is not guaranteed.\n"
            + "6. Misuse of the platform or content may result in suspension of access.\n\n"
            + "These terms are a configurable default and may be updated by the institute at any time.";

    private void seedFaqs() {
        if (faqItemRepository.count() > 0) {
            return;
        }
        int order = 0;
        faqItemRepository.save(faq("Do I need prior coding experience?",
                "No prior experience needed. Our courses start from the basics and progressively advance. "
                        + "Anyone with dedication can join and succeed.", "Admissions", order++));
        faqItemRepository.save(faq("Do you provide placement assistance?",
                "Yes — we provide resume building, mock interviews, and share profiles with our hiring network. "
                        + "However, job placement is not guaranteed.", "Placements", order++));
        faqItemRepository.save(faq("Can I attend offline classes?",
                "Yes! We offer both live online interactive sessions and in-person offline classroom training. "
                        + "Choose the mode that suits you best.", "Courses", order++));
        faqItemRepository.save(faq("Are there doubt-clearing sessions?",
                "Absolutely. We hold dedicated doubt-clearing sessions to ensure no concept is left unclear. "
                        + "You can also reach out to trainers between classes.", "Courses", order++));
        faqItemRepository.save(faq("How do I enroll in a course?",
                "Click \"Enroll Now\" on any course card or contact us directly on WhatsApp / phone. "
                        + "Our team will guide you through registration and batch selection.", "Admissions", order++));
        faqItemRepository.save(faq("How can I contact the institute?",
                "Call us on 8978866005 or 7893702635, or send a message through the Contact Us page. "
                        + "We'll get back to you as soon as possible.", "Support", order++));
    }

    private FaqItem faq(String question, String answer, String category, int sortOrder) {
        return FaqItem.builder()
                .question(question)
                .answer(answer)
                .category(category)
                .published(true)
                .sortOrder(sortOrder)
                .build();
    }

    private void seedDemoTestimonials() {
        if (reviewRepository.count() > 0) {
            return;
        }
        reviewRepository.save(demo("Demo Learner", "Placeholder feedback", 5,
                "This is a clearly-labelled demo testimonial shown until real, verified student feedback is published."));
        reviewRepository.save(demo("Demo Learner", "Placeholder feedback", 5,
                "Sample review content for layout purposes only — replace it with an approved student testimonial in the admin portal."));
        reviewRepository.save(demo("Demo Learner", "Placeholder feedback", 4,
                "Another placeholder entry so the testimonials page is not empty on first launch."));
    }

    private Review demo(String author, String context, int rating, String comment) {
        return Review.builder()
                .authorName(author)
                .contextLabel(context)
                .rating(rating)
                .comment(comment)
                .isApproved(true)
                .isDemo(true)
                .published(true)
                .build();
    }
}
