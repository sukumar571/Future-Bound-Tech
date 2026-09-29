package com.futureboundtech.dto;

import com.futureboundtech.enums.CourseLevel;
import com.futureboundtech.enums.TrainingMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

/** Institute settings form/read model for the admin portal. */
@Data
@Accessors(chain = true)
public class InstituteSettingsDto {

    private Long id;

    @NotBlank(message = "Institute name is required")
    private String name;

    private String tagLine;
    private String phone1;
    private String phone2;

    @Email(message = "Enter a valid email address")
    private String email;

    private String website;

    @Size(max = 500, message = "Address is too long")
    private String address;

    private String trainingModes;

    // ---- Branding ----
    private String logoUrl;
    private String faviconUrl;

    @Pattern(regexp = "^#([0-9A-Fa-f]{6})$", message = "Primary colour must be a hex value like #06B6D4")
    private String primaryColor;
    @Pattern(regexp = "^#([0-9A-Fa-f]{6})$", message = "Secondary colour must be a hex value like #0F172A")
    private String secondaryColor;
    @Pattern(regexp = "^#([0-9A-Fa-f]{6})$", message = "Accent colour must be a hex value like #F472B6")
    private String accentColor;

    /** Transient upload fields (not persisted; consumed in saveSettings). */
    private MultipartFile logoFile;
    private MultipartFile faviconFile;

    // ---- Social links ----
    private String facebookUrl;
    private String twitterUrl;
    private String instagramUrl;
    private String linkedinUrl;
    private String youtubeUrl;

    // ---- Fees + course defaults ----
    @Min(value = 0, message = "Registration fee cannot be negative")
    private BigDecimal defaultRegistrationFee;

    private String currency = "INR";

    @Min(value = 0, message = "Default duration cannot be negative")
    private Integer defaultDurationMonths;

    private CourseLevel defaultLevel;
    private TrainingMode defaultTrainingMode;
    private boolean defaultCertificateEligible = true;
    private boolean defaultPublicListed = false;

    // ---- Payment preferences (non-secret) ----
    private String paymentGatewayLabel;

    @Size(max = 1000, message = "Payment instructions are too long")
    private String paymentInstructions;

    /** Read-only indicator derived from the env-backed razorpay.enabled flag; never edited here. */
    private boolean paymentEnabled;

    // ---- Notifications ----
    private boolean allowCoupons = true;
    private boolean emailNotifications = true;
    private boolean whatsappNotifications = false;
    private boolean smsNotifications = false;

    // ---- Certificate branding ----
    @Size(max = 200)
    private String certificateSignatory;
    @Size(max = 200)
    private String certificateSignatoryTitle;
    @Size(max = 2000)
    private String certificateFooterNote;

    // ---- Legal content ----
    @Size(max = 20000, message = "Privacy policy is too long")
    private String privacyPolicy;
    @Size(max = 20000, message = "Terms and conditions are too long")
    private String termsAndConditions;
}
