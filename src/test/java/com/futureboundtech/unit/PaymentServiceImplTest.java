package com.futureboundtech.unit;

import com.futureboundtech.config.RazorpayConfig;
import com.futureboundtech.dto.VerifyPaymentRequest;
import com.futureboundtech.entity.Course;
import com.futureboundtech.entity.Enrollment;
import com.futureboundtech.entity.Payment;
import com.futureboundtech.entity.Student;
import com.futureboundtech.entity.User;
import com.futureboundtech.enums.EnrollmentStatus;
import com.futureboundtech.enums.PaymentStatus;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.exception.ResourceNotFoundException;
import com.futureboundtech.repository.*;
import com.futureboundtech.service.NotificationService;
import com.futureboundtech.service.impl.PaymentServiceImpl;
import com.razorpay.RazorpayClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the payment guard-rails that must never trust the browser:
 * signature verification, ownership, and webhook authentication. Failure paths
 * are exercised here with Mockito so they run without any gateway or database.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceImplTest {

    @Mock PaymentRepository paymentRepository;
    @Mock CourseRepository courseRepository;
    @Mock StudentRepository studentRepository;
    @Mock EnrollmentRepository enrollmentRepository;
    @Mock BatchRepository batchRepository;
    @Mock CouponRepository couponRepository;
    @Mock RazorpayConfig razorpayConfig;
    @Mock RazorpayClient razorpayClient;
    @Mock NotificationService notificationService;

    @InjectMocks PaymentServiceImpl paymentService;

    private Payment createdPayment(long ownerUserId, long studentId, long courseId, String orderId) {
        User owner = new User();
        owner.setId(ownerUserId);
        Student student = Student.builder().user(owner).build();
        student.setId(studentId);
        Course course = Course.builder().build();
        course.setId(courseId);
        return Payment.builder()
                .student(student)
                .course(course)
                .receiptNumber("FBT-" + orderId)
                .amount(new java.math.BigDecimal("999.00"))
                .razorpayOrderId(orderId)
                .status(PaymentStatus.CREATED)
                .build();
    }

    private static VerifyPaymentRequest request(String order, String payment, String signature) {
        VerifyPaymentRequest r = new VerifyPaymentRequest();
        r.setRazorpayOrderId(order);
        r.setRazorpayPaymentId(payment);
        r.setRazorpaySignature(signature);
        return r;
    }

    private static User user(long id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    @Test
    @DisplayName("isRazorpayEnabled simply reflects configuration")
    void enabledFlag() {
        when(razorpayConfig.isEnabled()).thenReturn(true);
        assertTrue(paymentService.isRazorpayEnabled());
    }

    @Test
    @DisplayName("incomplete verification payloads are refused")
    void incompleteRequest() {
        assertThrows(BusinessException.class, () -> paymentService.verifyAndActivate(null, user(1L)));
        assertThrows(BusinessException.class,
                () -> paymentService.verifyAndActivate(request("o1", "", "sig"), user(1L)));
    }

    @Test
    @DisplayName("verifying an order we never issued is a 404")
    void unknownOrder() {
        when(paymentRepository.findByRazorpayOrderId("nope")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.verifyAndActivate(request("nope", "p1", "sig"), user(1L)));
    }

    @Test
    @DisplayName("one student cannot redeem another student's payment")
    void wrongOwner() {
        Payment payment = createdPayment(1L, 10L, 5L, "o1");
        when(paymentRepository.findByRazorpayOrderId("o1")).thenReturn(Optional.of(payment));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.verifyAndActivate(request("o1", "p1", "sig"), user(2L)));
        assertTrue(ex.getMessage().toLowerCase().contains("belong"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("a tampered checkout signature marks the payment FAILED and never activates")
    void tamperedSignature() {
        when(razorpayConfig.getKeySecret()).thenReturn("test_key_secret");
        Payment payment = createdPayment(1L, 10L, 5L, "o1");
        when(paymentRepository.findByRazorpayOrderId("o1")).thenReturn(Optional.of(payment));
        when(enrollmentRepository.findByStudent_IdAndCourse_IdAndStatus(eq(10L), eq(5L), eq(EnrollmentStatus.PENDING_PAYMENT)))
                .thenReturn(Optional.<Enrollment>empty());

        assertThrows(BusinessException.class,
                () -> paymentService.verifyAndActivate(request("o1", "p1", "deadbeef"), user(1L)));

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verify(paymentRepository).save(payment);
        // Enrollment was never flipped to ACTIVE / SUCCESS.
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("webhooks require a payload and signature")
    void webhookBlank() {
        assertThrows(BusinessException.class, () -> paymentService.handleWebhook("", "sig"));
        assertThrows(BusinessException.class, () -> paymentService.handleWebhook("{}", ""));
    }

    @Test
    @DisplayName("webhooks with a bad signature are rejected before the payload is read")
    void webhookInvalidSignature() {
        when(razorpayConfig.getWebhookSecret()).thenReturn("test_webhook_secret");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.handleWebhook("{\"event\":\"payment.captured\"}", "not-the-signature"));
        assertTrue(ex.getMessage().toLowerCase().contains("signature"));
    }
}
