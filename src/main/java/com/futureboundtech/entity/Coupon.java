package com.futureboundtech.entity;

import com.futureboundtech.enums.CouponDiscountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import jakarta.validation.constraints.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "coupons")
public class Coupon extends BaseEntity {

    @NotBlank
    @Column(nullable = false, unique = true)
    private String code;

    /** Fixed rupee discount. Also used as the cap reference for percentage coupons. */
    @NotNull
    @Min(0)
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CouponDiscountType discountType = CouponDiscountType.FIXED;

    /** Percent off the base price (0-100). Only used when {@code discountType == PERCENTAGE}. */
    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    /** Upper bound on the discount produced by a percentage coupon (rupees). Null = no cap. */
    @Column(precision = 12, scale = 2)
    private BigDecimal maxDiscount;

    /** Optional start of the validity window. Null = valid from creation. */
    private LocalDateTime validFrom;

    private LocalDateTime validUntil;

    /** Maximum number of redemptions. Null = unlimited. */
    private Integer usageLimit;

    @Builder.Default
    private int usedCount = 0;

    /** Course ids this coupon is restricted to. Empty = applies to every course. */
    @ElementCollection
    @CollectionTable(name = "coupon_applicable_courses", joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "course_id")
    @Builder.Default
    private Set<Long> applicableCourseIds = new HashSet<>();

    @Builder.Default
    private boolean isActive = true;

    // ------------------------------------------------------------------
    // Behaviour
    // ------------------------------------------------------------------

    /**
     * Computes the rupee discount this coupon yields for {@code base}, never exceeding the
     * base amount. Percentage coupons honour the optional {@code maxDiscount} cap.
     */
    public BigDecimal computeDiscount(BigDecimal base) {
        if (base == null || base.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount;
        if (discountType == CouponDiscountType.PERCENTAGE && discountPercentage != null) {
            discount = base.multiply(discountPercentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (maxDiscount != null && discount.compareTo(maxDiscount) > 0) {
                discount = maxDiscount;
            }
        } else {
            discount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        }
        if (discount.compareTo(base) > 0) {
            discount = base;
        }
        if (discount.signum() < 0) {
            discount = BigDecimal.ZERO;
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    public boolean hasStarted() {
        return validFrom == null || !validFrom.isAfter(LocalDateTime.now());
    }

    public boolean hasExpired() {
        return validUntil != null && validUntil.isBefore(LocalDateTime.now());
    }

    public boolean isExhausted() {
        return usageLimit != null && usedCount >= usageLimit;
    }

    public boolean isApplicableTo(Long courseId) {
        return applicableCourseIds == null || applicableCourseIds.isEmpty()
                || (courseId != null && applicableCourseIds.contains(courseId));
    }

    /**
     * @return a human-readable reason this coupon cannot be used right now, or {@code null}
     *         when it is valid. Never trusts the browser — always evaluated server-side.
     */
    public String rejectionReason(Long courseId) {
        if (!isActive) {
            return "This coupon is no longer active.";
        }
        if (!hasStarted()) {
            return "This coupon is not active yet.";
        }
        if (hasExpired()) {
            return "This coupon has expired.";
        }
        if (isExhausted()) {
            return "This coupon has reached its usage limit.";
        }
        if (!isApplicableTo(courseId)) {
            return "This coupon is not valid for the selected course.";
        }
        return null;
    }
}
