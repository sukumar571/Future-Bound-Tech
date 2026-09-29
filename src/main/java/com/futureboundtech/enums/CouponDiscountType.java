package com.futureboundtech.enums;

/**
 * How a coupon reduces the payable amount.
 *
 * <ul>
 *   <li>{@code FIXED} — subtract {@code discountAmount} rupees from the base price.</li>
 *   <li>{@code PERCENTAGE} — subtract {@code discountPercentage} percent of the base price,
 *       optionally capped by {@code maxDiscount}.</li>
 * </ul>
 */
public enum CouponDiscountType {
    FIXED,
    PERCENTAGE
}
