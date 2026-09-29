package com.futureboundtech.dto;

import com.futureboundtech.enums.CouponDiscountType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Coupon read model and create/edit form for the admin portal. */
@Data
@Accessors(chain = true)
public class CouponDto {

    private Long id;

    @NotBlank(message = "Coupon code is required")
    private String code;

    @NotNull(message = "Discount is required")
    @DecimalMin(value = "0.00", message = "Discount cannot be negative")
    private BigDecimal discountAmount;

    @NotNull(message = "Choose a discount type")
    private CouponDiscountType discountType = CouponDiscountType.FIXED;

    @DecimalMin(value = "0.00", message = "Percentage cannot be negative")
    @DecimalMax(value = "100.00", message = "Percentage cannot exceed 100")
    private BigDecimal discountPercentage;

    @DecimalMin(value = "0.00", message = "Maximum discount cannot be negative")
    private BigDecimal maxDiscount;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime validFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime validUntil;

    @DecimalMin(value = "0", message = "Usage limit cannot be negative")
    private Integer usageLimit;

    private int usedCount;

    /** Course ids the coupon is restricted to. Empty = all courses. */
    private List<Long> applicableCourseIds = new ArrayList<>();

    private boolean active = true;

    private String formattedDiscount;
    private String discountSummary;
    private String statusLabel;
    private LocalDateTime createdAt;
}
