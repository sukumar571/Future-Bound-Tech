package com.futureboundtech.dto;

import com.futureboundtech.enums.TrainingMode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * Server-computed enrollment/checkout summary for a course (and optional batch),
 * including coupon evaluation and the final payable amount. The values shown to
 * the student always come from here — never from the browser.
 */
@Data
@Accessors(chain = true)
public class EnrollmentQuoteDto {

    private String courseSlug;
    private Long courseId;
    private String courseTitle;
    private String trainerName;
    private String thumbnailUrl;

    private Long batchId;
    private String batchName;
    private String batchMeta;          // e.g. "Starts 12 Sep 2026 · 20 seats left"
    private TrainingMode trainingMode;
    private String trainingModeLabel;

    private boolean feeConfigured;
    private BigDecimal baseAmount;
    private BigDecimal discountAmount;
    private BigDecimal payableAmount;
    private String baseAmountLabel;
    private String discountLabel;
    private String payableLabel;

    private String couponCode;
    private boolean couponApplied;
    private String couponMessage;

    private boolean razorpayEnabled;
    private String errorMessage;
}
