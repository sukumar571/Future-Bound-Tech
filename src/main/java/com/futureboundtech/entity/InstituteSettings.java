package com.futureboundtech.entity;

import com.futureboundtech.enums.CourseLevel;
import com.futureboundtech.enums.TrainingMode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "institutesettingss")
public class InstituteSettings extends BaseEntity {

    @NotBlank
    @Column(nullable = false)
    private String name;
    
    private String tagLine;
    private String phone1;
    private String phone2;
    private String email;

    private String website;

    @Size(max = 500)
    @Column(length = 500)
    private String address;

    @Size(max = 500)
    @Column(length = 500)
    private String logoUrl;

    @Size(max = 500)
    @Column(length = 500)
    private String faviconUrl;

    /** Brand theme colours (hex, e.g. #06B6D4). Optional; blank keeps the built-in theme. */
    @Size(max = 9)
    private String primaryColor;
    @Size(max = 9)
    private String secondaryColor;
    @Size(max = 9)
    private String accentColor;

    /** Free-text training modes shown publicly, e.g. "Online + Offline". */
    @Size(max = 200)
    private String trainingModes;

    private String facebookUrl;
    private String twitterUrl;
    private String instagramUrl;
    private String linkedinUrl;
    private String youtubeUrl;

    // ---- Course creation defaults (prefill the admin new-course form) ----
    @Min(0)
    private BigDecimal defaultRegistrationFee;

    @Builder.Default
    private String currency = "INR";

    @Min(0)
    private Integer defaultDurationMonths;

    @Enumerated(EnumType.STRING)
    private CourseLevel defaultLevel;

    @Enumerated(EnumType.STRING)
    private TrainingMode defaultTrainingMode;

    @Builder.Default
    private boolean defaultCertificateEligible = true;

    @Builder.Default
    private boolean defaultPublicListed = false;

    // ---- Payment preferences (never store gateway secrets here; those live in env) ----
    @Size(max = 100)
    private String paymentGatewayLabel;

    @Size(max = 1000)
    @Column(length = 1000)
    private String paymentInstructions;

    // ---- Notification toggles ----
    @Builder.Default
    private boolean allowCoupons = true;

    @Builder.Default
    private boolean emailNotifications = true;

    @Builder.Default
    private boolean whatsappNotifications = false;

    @Builder.Default
    private boolean smsNotifications = false;

    // ---- Certificate branding ----
    @Size(max = 200)
    private String certificateSignatory;

    @Size(max = 200)
    private String certificateSignatoryTitle;

    @Column(columnDefinition = "TEXT")
    private String certificateFooterNote;

    // ---- Legal content ----
    @Column(columnDefinition = "TEXT")
    private String privacyPolicy;

    @Column(columnDefinition = "TEXT")
    private String termsAndConditions;

}
