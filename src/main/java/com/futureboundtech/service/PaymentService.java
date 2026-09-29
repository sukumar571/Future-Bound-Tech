package com.futureboundtech.service;

import com.futureboundtech.dto.EnrollmentQuoteDto;
import com.futureboundtech.dto.PaymentDto;
import com.futureboundtech.dto.PaymentOrderDto;
import com.futureboundtech.dto.VerifyPaymentRequest;
import com.futureboundtech.entity.User;
import com.futureboundtech.enums.PaymentStatus;

import java.util.List;
import java.util.Map;

public interface PaymentService {

    boolean isRazorpayEnabled();

    String getPublicKey();

    /**
     * Server-side checkout summary for a course (and optional batch) including coupon
     * evaluation and the final payable amount. Never trusts browser-supplied prices.
     */
    EnrollmentQuoteDto quote(String courseSlug, Long batchId, String couponCode, User user);

    /**
     * Creates a Razorpay order for the course/batch and persists a CREATED payment plus a
     * PENDING_PAYMENT enrollment. When the gateway is disabled the payment is settled
     * server-side and the enrollment is activated immediately (demo mode).
     */
    PaymentOrderDto createOrder(String courseSlug, Long batchId, User user, String couponCode);

    /** Verifies the checkout signature server-side and, only on success, activates enrollment. */
    PaymentDto verifyAndActivate(VerifyPaymentRequest request, User user);

    /**
     * Confirms a demo (placeholder) payment created while the gateway is disabled: marks the
     * order SUCCESS and activates the reserved enrollment. Ownership is enforced and the
     * operation is idempotent. Rejected when the live gateway is enabled.
     */
    PaymentDto confirmDemoPayment(String orderId, User user);

    /** Validates and processes a Razorpay webhook event. */
    void handleWebhook(String payload, String signature);

    List<PaymentDto> historyFor(User user);

    /** Fetches a receipt, enforcing that a student can only view their own payment. */
    PaymentDto receipt(Long paymentId, User user);

    PaymentDto findByReceiptNumber(String receiptNumber, User user);

    List<PaymentDto> adminReport(PaymentStatus status);

    Map<String, Object> adminSummary();
}
