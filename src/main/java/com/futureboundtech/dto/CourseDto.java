package com.futureboundtech.dto;

import com.futureboundtech.enums.CourseCategory;
import com.futureboundtech.enums.CourseLevel;
import com.futureboundtech.enums.CourseStatus;
import com.futureboundtech.enums.TrainingMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Accessors(chain = true)
public class CourseDto {

    private Long id;

    @NotBlank(message = "Title is required")
    @Size(max = 180, message = "Title must be 180 characters or fewer")
    private String title;

    @Size(max = 200, message = "Slug must be 200 characters or fewer")
    private String slug;

    @NotBlank(message = "Short description is required")
    @Size(max = 500, message = "Short description must be 500 characters or fewer")
    private String shortDescription;

    @NotBlank(message = "Detailed description is required")
    @Size(max = 8000, message = "Detailed description is too long")
    private String detailedDescription;

    private String thumbnailPath;
    private String thumbnailUrl;

    @NotNull(message = "Category is required")
    private CourseCategory category;

    @NotNull(message = "Skill level is required")
    private CourseLevel level;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 month")
    private Integer durationMonths;

    @DecimalMin(value = "0.00", message = "Course fee cannot be negative")
    private BigDecimal fee;

    @DecimalMin(value = "0.00", message = "Discount fee cannot be negative")
    private BigDecimal discountFee;

    @NotNull(message = "Training mode is required")
    private TrainingMode trainingMode;

    private Long trainerId;
    private String trainerName;
    private String trainerExpertise;

    @NotNull(message = "Status is required")
    private CourseStatus status = CourseStatus.DRAFT;

    @Size(max = 4000, message = "Learning outcomes are too long")
    private String learningOutcomes;

    @Size(max = 4000, message = "Prerequisites are too long")
    private String prerequisites;

    @Size(max = 4000, message = "Projects are too long")
    private String projects;

    private boolean certificateEligible = true;
    private boolean publicListed = false;

    private MultipartFile thumbnailFile;

    private String themeKey;
    private String durationLabel;
    private boolean feeConfigured;
    private boolean discountConfigured;
    private String formattedFee;
    private String formattedDiscountFee;

    public List<String> getLearningOutcomeItems() {
        return splitLines(learningOutcomes);
    }

    public List<String> getPrerequisiteItems() {
        return splitLines(prerequisites);
    }

    public List<String> getProjectItems() {
        return splitLines(projects);
    }

    private List<String> splitLines(String value) {
        if (value == null || value.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(value.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }
}
