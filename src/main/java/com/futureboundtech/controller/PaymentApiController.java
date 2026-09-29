package com.futureboundtech.controller;

import com.futureboundtech.dto.PaymentDto;
import com.futureboundtech.dto.PaymentOrderDto;
import com.futureboundtech.dto.VerifyPaymentRequest;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.exception.ResourceNotFoundException;
import com.futureboundtech.security.CustomUserDetails;
import com.futureboundtech.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Razorpay payment API.
 *
 * <p>Amounts are always calculated and orders always created on the server; the
 * client can never supply a price. Checkout and webhook signatures are verified
 * server-side, and an enrollment is only ever activated from a verified, settled
 * payment — never from a raw frontend callback.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentApiController {

    private final PaymentService paymentService;

    /**
     * Creates a Razorpay order for a course/batch (with optional coupon) and returns
     * the payload needed to open Razorpay Checkout. Requires a logged-in student.
     */
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestParam("slug") String slug,
                                         @RequestParam(value = "batchId", required = false) Long batchId,
                                         @RequestParam(value = "coupon", required = false) String coupon,
                                         @AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Please log in to make a payment."));
        }
        try {
            PaymentOrderDto order = paymentService.createOrder(slug, batchId, principal.getUser(), coupon);
            return ResponseEntity.ok(order);
        } catch (BusinessException | ResourceNotFoundException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    /**
     * Verifies the Razorpay checkout signature server-side and, only on success,
     * activates the associated enrollment. Idempotent — a settled order returns as-is.
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody VerifyPaymentRequest request,
                                    @AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Please log in to verify a payment."));
        }
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            PaymentDto dto = paymentService.verifyAndActivate(request, principal.getUser());
            response.put("success", true);
            response.put("status", dto.getStatus());
            response.put("receiptNumber", dto.getReceiptNumber());
            response.put("courseSlug", dto.getCourseSlug());
            return ResponseEntity.ok(response);
        } catch (BusinessException | ResourceNotFoundException ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Confirms a demo (placeholder) payment created while the gateway is disabled, activating
     * the reserved enrollment. Ownership-checked and idempotent; rejected when live.
     */
    @PostMapping("/demo-confirm")
    public ResponseEntity<?> demoConfirm(@RequestParam("orderId") String orderId,
                                         @AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Please log in to confirm a payment."));
        }
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            PaymentDto dto = paymentService.confirmDemoPayment(orderId, principal.getUser());
            response.put("success", true);
            response.put("status", dto.getStatus());
            response.put("receiptNumber", dto.getReceiptNumber());
            response.put("courseSlug", dto.getCourseSlug());
            return ResponseEntity.ok(response);
        } catch (BusinessException | ResourceNotFoundException ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Razorpay webhook receiver. The raw request body is authenticated against the
     * configured webhook secret before any event is processed. Events are handled
     * idempotently so replays never double-activate an enrollment.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody String payload,
                                          @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        try {
            paymentService.handleWebhook(payload, signature);
            return ResponseEntity.ok("ok");
        } catch (BusinessException ex) {
            log.warn("Rejected Razorpay webhook: {}", ex.getMessage());
            return ResponseEntity.badRequest().body("invalid");
        }
    }
}
