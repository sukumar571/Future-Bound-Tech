package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * Payload returned to the browser so Razorpay Checkout can be opened. The
 * {@code razorpayKeySecret} is intentionally never included here.
 */
@Data
@Accessors(chain = true)
public class PaymentOrderDto {

    private Long id;
    private String razorpayKeyId;
    private String orderId;
    /** Amount in paise (smallest unit) — required by Razorpay Checkout. */
    private long amountInPaise;
    private String currency;
    private String receiptNumber;
    private BigDecimal amount;

    private String courseSlug;
    private Long batchId;
    private Long enrollmentId;

    /** True when the gateway is disabled and this is a demo (placeholder) payment. */
    private boolean demo;

    /**
     * True when the enrollment has already been settled/activated server-side (free seat
     * or a completed demo payment). False when the student still has to pay — the browser
     * then shows the demo QR page or the Razorpay gateway.
     */
    private boolean settled;

    private String merchantName;
    private String courseTitle;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
}
