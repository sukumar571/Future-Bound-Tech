package com.futureboundtech.integration;

import com.futureboundtech.dto.EnrollmentQuoteDto;
import com.futureboundtech.entity.*;
import com.futureboundtech.enums.*;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.repository.EnrollmentRepository;
import com.futureboundtech.repository.PaymentRepository;
import com.futureboundtech.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end payment & enrollment behaviour against the real service graph.
 * The gateway runs in DEMO mode (razorpay.enabled=false): order creation reserves a
 * PENDING seat and a demo payment is settled only when the student confirms it via
 * confirmDemoPayment(); the gateway signature path is exercised separately with a
 * seeded CREATED payment and a correctly-signed request.
 */
class PaymentFlowIntegrationTest extends AbstractIntegrationTest {

    @org.springframework.beans.factory.annotation.Autowired PaymentService paymentService;
    @org.springframework.beans.factory.annotation.Autowired PaymentRepository paymentRepository;
    @org.springframework.beans.factory.annotation.Autowired EnrollmentRepository enrollmentRepository;

    private Course paidCourse(String slug) {
        return courseRepository.save(Course.builder()
                .title("Paid " + slug).slug(slug).shortDescription("s").description("d")
                .category(CourseCategory.WEB_DEVELOPMENT).level(CourseLevel.BEGINNER)
                .durationInDays(30).fee(new BigDecimal("4999")).trainingMode(TrainingMode.ONLINE)
                .status(CourseStatus.PUBLISHED).publicListed(true).build());
    }

    private static String sign(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        StringBuilder hex = new StringBuilder();
        for (byte b : mac.doFinal(data.getBytes(StandardCharsets.UTF_8))) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    @Test
    @DisplayName("quote returns the server-computed payable amount (never the browser's)")
    void quoteIsServerSide() {
        paidCourse("quote-course");
        User student = seedStudent("quote@test.com", "password123");
        EnrollmentQuoteDto q = paymentService.quote("quote-course", null, null, student);
        assertEquals(0, q.getPayableAmount().compareTo(new BigDecimal("4999")));
        assertEquals(0, q.getDiscountAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("demo checkout reserves a seat, then the confirmed payment activates it")
    void demoOrderActivatesEnrollment() {
        Course c = paidCourse("demo-course");
        User student = seedStudent("demo@test.com", "password123");
        com.futureboundtech.dto.PaymentOrderDto order =
                paymentService.createOrder("demo-course", null, student, null);
        assertNotNull(order);
        assertTrue(order.isDemo(), "gateway is disabled so this is a demo order");
        assertFalse(order.isSettled(), "a payable demo order is not settled until confirmed");

        Student s = studentRepository.findByUser_Id(student.getId()).orElseThrow();
        assertFalse(enrollmentRepository.existsByStudent_IdAndCourse_IdAndStatusIn(
                        s.getId(), c.getId(), List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED)),
                "the seat must stay locked before payment confirmation");

        paymentService.confirmDemoPayment(order.getOrderId(), student);
        assertTrue(enrollmentRepository.existsByStudent_IdAndCourse_IdAndStatusIn(
                        s.getId(), c.getId(), List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED)),
                "confirming the demo payment must unlock the seat");
    }

    @Test
    @DisplayName("a completed course cannot be re-purchased (duplicate prevention)")
    void duplicatePurchaseBlocked() {
        paidCourse("dup-course");
        User student = seedStudent("dup@test.com", "password123");
        com.futureboundtech.dto.PaymentOrderDto order =
                paymentService.createOrder("dup-course", null, student, null);
        paymentService.confirmDemoPayment(order.getOrderId(), student);
        assertThrows(BusinessException.class,
                () -> paymentService.createOrder("dup-course", null, student, null));
    }

    @Test
    @DisplayName("a correctly-signed gateway payment activates the reserved PENDING seat")
    void gatewaySignatureHappyPath() throws Exception {
        Course c = paidCourse("gw-course");
        User student = seedStudent("gw@test.com", "password123");
        Student s = studentRepository.findByUser_Id(student.getId()).orElseThrow();

        // Seed the state a real order-creation would leave behind.
        Enrollment pending = enrollmentRepository.save(Enrollment.builder()
                .student(s).course(c).status(EnrollmentStatus.PENDING_PAYMENT)
                .amount(new BigDecimal("4999")).build());
        Payment created = paymentRepository.save(Payment.builder()
                .student(s).course(c).receiptNumber("RCT-GW")
                .amount(new BigDecimal("4999")).razorpayOrderId("order_GW")
                .status(PaymentStatus.CREATED).build());

        String signature = sign("order_GW" + "|" + "pay_GW", "test_key_secret");
        com.futureboundtech.dto.VerifyPaymentRequest req = new com.futureboundtech.dto.VerifyPaymentRequest();
        req.setRazorpayOrderId("order_GW");
        req.setRazorpayPaymentId("pay_GW");
        req.setRazorpaySignature(signature);

        paymentService.verifyAndActivate(req, student);

        Optional<Payment> p = paymentRepository.findByRazorpayOrderId("order_GW");
        assertTrue(p.isPresent() && p.get().getStatus() == PaymentStatus.SUCCESS);
        assertTrue(enrollmentRepository.existsByStudent_IdAndCourse_IdAndStatusIn(
                s.getId(), c.getId(), List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED)));
    }

    @Test
    @DisplayName("an incorrectly-signed gateway payment is refused and never activated")
    void gatewaySignatureRejected() {
        Course c = paidCourse("gw-bad-course");
        User student = seedStudent("gw-bad@test.com", "password123");
        Student s = studentRepository.findByUser_Id(student.getId()).orElseThrow();
        enrollmentRepository.save(Enrollment.builder().student(s).course(c)
                .status(EnrollmentStatus.PENDING_PAYMENT).amount(new BigDecimal("4999")).build());
        paymentRepository.save(Payment.builder().student(s).course(c).receiptNumber("RCT-BAD")
                .amount(new BigDecimal("4999")).razorpayOrderId("order_BAD").status(PaymentStatus.CREATED).build());

        com.futureboundtech.dto.VerifyPaymentRequest req = new com.futureboundtech.dto.VerifyPaymentRequest();
        req.setRazorpayOrderId("order_BAD");
        req.setRazorpayPaymentId("pay_BAD");
        req.setRazorpaySignature("00".repeat(32));

        assertThrows(BusinessException.class, () -> paymentService.verifyAndActivate(req, student));
        assertFalse(enrollmentRepository.existsByStudent_IdAndCourse_IdAndStatusIn(
                s.getId(), c.getId(), List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED)));
    }
}
