package com.futureboundtech.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.futureboundtech.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnore
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnore
    private Course course;

    /** Optional batch the student chose at checkout; links the resulting enrollment. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    @JsonIgnore
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    @JsonIgnore
    private Coupon coupon;

    /** Enrollment activated by this payment (set once the payment is verified as SUCCESS). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    @JsonIgnore
    private Enrollment enrollment;

    /** Institute-owned receipt reference shared with Razorpay. */
    @Column(nullable = false, unique = true)
    private String receiptNumber;

    @Column(unique = true)
    private String razorpayOrderId;

    @Column(unique = true)
    private String razorpayPaymentId;

    @Column(length = 200)
    private String razorpaySignature;

    /**
     * Server-side signature verification result: {@code TRUE} once the Razorpay checkout
     * signature has been validated with the API key secret, {@code FALSE} when it failed,
     * {@code null} while unverified. Enrollment is only ever activated when this is TRUE
     * (or when the payment is settled by a verified webhook).
     */
    private Boolean signatureValid;

    private String razorpayRefundId;

    /** Amount actually charged (after coupon), in rupees. */
    @NotNull
    @Min(0)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** Course fee used as the basis of this payment, in rupees. */
    @Column(precision = 12, scale = 2)
    private BigDecimal baseAmount;

    /** Discount applied through a coupon, in rupees. */
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.CREATED;

    private String paymentMethod;

    @Column(length = 1000)
    private String failureReason;

    private LocalDateTime paidAt;

    private LocalDateTime refundedAt;

    public boolean isSettled() {
        return status != null && status.isSettled();
    }
}
