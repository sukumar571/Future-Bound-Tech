package com.futureboundtech.entity;

import com.futureboundtech.enums.CourseCategory;
import com.futureboundtech.enums.CourseLevel;
import com.futureboundtech.enums.CourseStatus;
import com.futureboundtech.enums.TrainingMode;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "courses")
public class Course extends BaseEntity {

    @NotBlank
    @Column(nullable = false)
    private String title;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String slug;

    @Size(max = 500)
    @Column(length = 500)
    private String shortDescription;

    @Size(max = 8000)
    @Column(columnDefinition = "TEXT")
    private String description;

    private String thumbnailPath;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseCategory category;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseLevel level;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer durationInDays;

    /**
     * Catalog fee. Zero means the institute has not published a fee yet.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal discountFee;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrainingMode trainingMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id")
    private Trainer trainer;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CourseStatus status = CourseStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String learningOutcomes;

    @Column(columnDefinition = "TEXT")
    private String prerequisites;

    @Column(columnDefinition = "TEXT")
    private String projects;

    @Builder.Default
    private boolean certificateEligible = true;

    @Builder.Default
    @Column(name = "is_public")
    private boolean publicListed = false;

    @Builder.Default
    private boolean deleted = false;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CourseModule> modules = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Batch> batches = new ArrayList<>();

    public boolean hasConfiguredFee() {
        return fee != null && fee.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean hasDiscount() {
        return hasConfiguredFee()
                && discountFee != null
                && discountFee.compareTo(BigDecimal.ZERO) > 0
                && discountFee.compareTo(fee) < 0;
    }
}
