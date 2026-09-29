package com.futureboundtech.dto;

import lombok.Data;

/**
 * Body posted by the Razorpay Checkout success handler. The backend re-verifies
 * these values against the shared secret before activating any enrollment.
 */
@Data
public class VerifyPaymentRequest {

    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
}
