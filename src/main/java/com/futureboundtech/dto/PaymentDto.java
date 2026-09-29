package com.futureboundtech.dto;

import com.futureboundtech.enums.PaymentStatus;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class PaymentDto {

    private Long id;
    private String receiptNumber;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpayRefundId;
    private Boolean signatureValid;

    private Long courseId;
    private String courseTitle;
    private String courseSlug;

    private Long studentId;
    private String studentName;
    private String studentEmail;

    private Long enrollmentId;

    private BigDecimal amount;
    private BigDecimal baseAmount;
    private BigDecimal discountAmount;
    private String currency;
    private String couponCode;

    private PaymentStatus status;
    private String statusLabel;
    private String paymentMethod;
    private String failureReason;

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;

    private String formattedAmount;
    private String formattedBaseAmount;
    private String formattedDiscount;
}
