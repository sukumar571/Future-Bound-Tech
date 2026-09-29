package com.futureboundtech.unit;

import com.futureboundtech.entity.Coupon;
import com.futureboundtech.enums.CouponDiscountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure domain tests for {@link Coupon}: server-side discount maths and the
 * applicability rules that guard coupon abuse at checkout.
 */
class CouponTest {

    private static int cmp(BigDecimal a, String b) {
        return a.compareTo(new BigDecimal(b));
    }

    @Test
    @DisplayName("FIXED coupon yields its amount, clamped to the base price")
    void fixedDiscount() {
        Coupon c = Coupon.builder().code("SAVE100").discountType(CouponDiscountType.FIXED)
                .discountAmount(new BigDecimal("100")).build();
        assertEquals(0, cmp(c.computeDiscount(new BigDecimal("1000")), "100.00"));
        // Discount larger than the base is clamped to the base.
        assertEquals(0, cmp(c.computeDiscount(new BigDecimal("50")), "50.00"));
    }

    @Test
    @DisplayName("PERCENTAGE coupon honours the optional maxDiscount cap")
    void percentageDiscount() {
        Coupon uncapped = Coupon.builder().code("P10").discountType(CouponDiscountType.PERCENTAGE)
                .discountPercentage(new BigDecimal("10")).build();
        assertEquals(0, cmp(uncapped.computeDiscount(new BigDecimal("1000")), "100.00"));

        Coupon capped = Coupon.builder().code("P50CAP20").discountType(CouponDiscountType.PERCENTAGE)
                .discountPercentage(new BigDecimal("50")).maxDiscount(new BigDecimal("20")).build();
        assertEquals(0, cmp(capped.computeDiscount(new BigDecimal("1000")), "20.00"));
    }

    @Test
    @DisplayName("zero / negative / null base produces no discount")
    void nonPositiveBase() {
        Coupon c = Coupon.builder().code("X").discountType(CouponDiscountType.FIXED)
                .discountAmount(new BigDecimal("100")).build();
        assertEquals(0, cmp(c.computeDiscount(null), "0"));
        assertEquals(0, cmp(c.computeDiscount(BigDecimal.ZERO), "0"));
        assertEquals(0, cmp(c.computeDiscount(new BigDecimal("-5")), "0"));
    }

    @Test
    @DisplayName("rejectionReason surfaces every ineligibility case and null when valid")
    void rejectionReasons() {
        Coupon inactive = Coupon.builder().code("A").isActive(false).build();
        assertNotNull(inactive.rejectionReason(1L));

        Coupon notStarted = Coupon.builder().code("B").validFrom(LocalDateTime.now().plusDays(3)).build();
        assertTrue(notStarted.rejectionReason(1L).toLowerCase().contains("not active yet"));

        Coupon expired = Coupon.builder().code("C").validUntil(LocalDateTime.now().minusDays(1)).build();
        assertTrue(expired.rejectionReason(1L).toLowerCase().contains("expired"));

        Coupon exhausted = Coupon.builder().code("D").usageLimit(1).usedCount(1).build();
        assertTrue(exhausted.isExhausted());
        assertTrue(exhausted.rejectionReason(1L).toLowerCase().contains("usage limit"));

        Coupon restricted = Coupon.builder().code("E").applicableCourseIds(Set.of(7L)).build();
        assertTrue(restricted.rejectionReason(99L).toLowerCase().contains("not valid for the selected course"));

        Coupon ok = Coupon.builder().code("F").build();
        assertNull(ok.rejectionReason(1L));
    }

    @Test
    @DisplayName("unrestricted coupons apply to any course; restricted ones only to listed ids")
    void applicability() {
        Coupon any = Coupon.builder().code("ANY").build();
        assertTrue(any.isApplicableTo(123L));

        Coupon only7 = Coupon.builder().code("ONLY7").applicableCourseIds(Set.of(7L)).build();
        assertTrue(only7.isApplicableTo(7L));
        assertFalse(only7.isApplicableTo(8L));
    }
}
