package com.futureboundtech.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futureboundtech.config.RazorpayConfig;
import com.futureboundtech.dto.EnrollmentQuoteDto;
import com.futureboundtech.dto.PaymentDto;
import com.futureboundtech.dto.PaymentOrderDto;
import com.futureboundtech.dto.VerifyPaymentRequest;
import com.futureboundtech.entity.*;
import com.futureboundtech.enums.BatchEnrollmentStatus;
import com.futureboundtech.enums.CourseStatus;
import com.futureboundtech.enums.EnrollmentStatus;
import com.futureboundtech.enums.PaymentStatus;
import com.futureboundtech.enums.Role;
import com.futureboundtech.enums.TrainingMode;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.exception.ResourceNotFoundException;
import com.futureboundtech.repository.*;
import com.futureboundtech.service.NotificationService;
import com.futureboundtech.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String MERCHANT_NAME = "Future Bound Tech";
    private static final String CURRENCY = "INR";
    private static final int MIN_PAISE = 100; // Razorpay requires >= ₹1
    private static final DateTimeFormatter BATCH_DATE = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final PaymentRepository paymentRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final BatchRepository batchRepository;
    private final CouponRepository couponRepository;
    private final RazorpayConfig razorpayConfig;
    private final RazorpayClient razorpayClient;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void logMode() {
        if (!razorpayConfig.isEnabled()) {
            log.warn("Razorpay is running in placeholder mode (razorpay.enabled=false). "
                    + "Set RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET / RAZORPAY_WEBHOOK_SECRET and enable it to take real payments.");
        }
    }

    @Override
    public boolean isRazorpayEnabled() {
        return razorpayConfig.isEnabled();
    }

    @Override
    public String getPublicKey() {
        return razorpayConfig.getKeyId();
    }

    // ---------------------------------------------------------------------
    // Checkout quote
    // ---------------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public EnrollmentQuoteDto quote(String courseSlug, Long batchId, String couponCode, User user) {
        Course course = requirePublishedCourse(courseSlug);
        EnrollmentQuoteDto quote = new EnrollmentQuoteDto()
                .setCourseSlug(course.getSlug())
                .setCourseId(course.getId())
                .setCourseTitle(course.getTitle())
                .setTrainerName(trainerName(course))
                .setFeeConfigured(course.hasConfiguredFee())
                .setRazorpayEnabled(razorpayConfig.isEnabled());

        Batch batch = batchId == null ? null
                : batchRepository.findById(batchId)
                    .filter(b -> b.getCourse().getId().equals(course.getId()))
                    .orElse(null);
        TrainingMode mode = batch != null && batch.getMode() != null ? batch.getMode() : course.getTrainingMode();
        quote.setTrainingMode(mode).setTrainingModeLabel(modeLabel(mode));
        if (batch != null) {
            quote.setBatchId(batch.getId())
                    .setBatchName(batch.getBatchName())
                    .setBatchMeta(batchMeta(batch));
        }

        BigDecimal baseAmount = course.hasDiscount() ? course.getDiscountFee() : course.getFee();
        if (baseAmount == null) {
            baseAmount = BigDecimal.ZERO;
        }
        BigDecimal discount = BigDecimal.ZERO;
        if (!isBlank(couponCode)) {
            try {
                Coupon coupon = resolveCoupon(couponCode, course.getId());
                discount = coupon.computeDiscount(baseAmount);
                quote.setCouponApplied(true).setCouponMessage("Coupon " + coupon.getCode() + " applied.");
            } catch (BusinessException ex) {
                quote.setCouponMessage(ex.getMessage());
            }
        }
        BigDecimal payable = baseAmount.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        if (payable.signum() < 0) {
            payable = BigDecimal.ZERO;
        }
        quote.setCouponCode(isBlank(couponCode) ? null : couponCode.trim())
                .setBaseAmount(baseAmount)
                .setDiscountAmount(discount)
                .setPayableAmount(payable)
                .setBaseAmountLabel(formatInr(baseAmount))
                .setDiscountLabel(formatInr(discount))
                .setPayableLabel(formatInr(payable));
        return quote;
    }

    // ---------------------------------------------------------------------
    // Order creation
    // ---------------------------------------------------------------------
    @Override
    @Transactional
    public PaymentOrderDto createOrder(String courseSlug, Long batchId, User user, String couponCode) {
        if (user == null || user.getRole() != Role.STUDENT) {
            throw new BusinessException("Only student accounts can make payments.");
        }
        Course course = requirePublishedCourse(courseSlug);
        if (!course.hasConfiguredFee()) {
            throw new BusinessException("The institute has not published a fee for this course yet. Please contact support.");
        }

        Student student = studentRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new BusinessException("Student profile is missing. Please contact support."));

        // Duplicate prevention: an active/completed enrollment cannot be re-purchased.
        if (enrollmentRepository.existsByStudent_IdAndCourse_IdAndStatusIn(student.getId(), course.getId(),
                List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED))) {
            throw new BusinessException("You are already enrolled in this course.");
        }
        if (paymentRepository.existsByStudent_IdAndCourse_IdAndStatus(student.getId(), course.getId(), PaymentStatus.SUCCESS)) {
            throw new BusinessException("Payment for this course has already been completed.");
        }

        Batch batch = requireEnrollableBatch(course, batchId, student.getId());

        BigDecimal baseAmount = course.hasDiscount() ? course.getDiscountFee() : course.getFee();
        Coupon coupon = resolveCoupon(couponCode, course.getId());
        BigDecimal discount = coupon == null ? BigDecimal.ZERO : coupon.computeDiscount(baseAmount);
        BigDecimal payable = baseAmount.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        TrainingMode mode = batch != null && batch.getMode() != null ? batch.getMode() : course.getTrainingMode();

        // Reserve the seat with a PENDING_PAYMENT enrollment (activated only after payment).
        Enrollment enrollment = upsertPendingEnrollment(student, course, batch, mode, payable);

        Payment payment = Payment.builder()
                .student(student)
                .course(course)
                .batch(batch)
                .coupon(coupon)
                .receiptNumber(generateReceiptNumber())
                .baseAmount(baseAmount)
                .discountAmount(discount)
                .amount(payable)
                .currency(CURRENCY)
                .status(PaymentStatus.CREATED)
                .build();

        long paise = payable.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();

        if (paise < MIN_PAISE) {
            // Fully discounted / free seat: nothing to collect, settle server-side.
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            payment.setPaymentMethod("FREE");
            payment.setRazorpayOrderId("FREE-" + UUID.randomUUID());
            Payment saved = paymentRepository.save(payment);
            activateEnrollment(saved);
            log.info("Free redemption settled for course {} student {}", course.getId(), student.getId());
            return toOrderDto(saved, 0L, saved.getRazorpayOrderId(), true, true, enrollment.getId());
        }

        if (!razorpayConfig.isEnabled()) {
            // Demo mode (gateway disabled) with a real amount: reserve the seat and hand the
            // browser a placeholder UPI QR. The enrollment stays PENDING until the student
            // confirms payment via confirmDemoPayment(). No real money moves here.
            payment.setStatus(PaymentStatus.CREATED);
            payment.setRazorpayOrderId("DEMO-" + UUID.randomUUID());
            Payment saved = paymentRepository.save(payment);
            log.info("Demo payment order created for course {} student {}", course.getId(), student.getId());
            return toOrderDto(saved, paise, saved.getRazorpayOrderId(), true, false, enrollment.getId());
        }

        String orderId = createRazorpayOrder(payment.getReceiptNumber(), paise, course.getSlug());
        payment.setRazorpayOrderId(orderId);
        Payment saved = paymentRepository.save(payment);
        return toOrderDto(saved, paise, orderId, false, false, enrollment.getId());
    }

    private Course requirePublishedCourse(String courseSlug) {
        Course course = courseRepository.findBySlugAndDeletedFalse(courseSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found."));
        if (course.getStatus() != CourseStatus.PUBLISHED || !course.isPublicListed()) {
            throw new BusinessException("This course is not open for enrollment.");
        }
        return course;
    }

    /** Validates that the (optional) batch belongs to the course, is published, open and has seats left. */
    private Batch requireEnrollableBatch(Course course, Long batchId, Long studentId) {
        if (batchId == null) {
            return null;
        }
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException("Select a valid batch."));
        if (!batch.getCourse().getId().equals(course.getId())) {
            throw new BusinessException("The selected batch does not belong to this course.");
        }
        if (!batch.isPublished()) {
            throw new BusinessException("The selected batch is not open for enrollment.");
        }
        if (batch.getEnrollmentStatus() == BatchEnrollmentStatus.CLOSED) {
            throw new BusinessException("Enrollment for the selected batch is closed.");
        }
        if (batch.getMaxSeats() != null) {
            long holding = enrollmentRepository.countSeatsHeldByBatch(batch.getId());
            if (holding >= batch.getMaxSeats()) {
                throw new BusinessException("The selected batch is full. Please choose another batch.");
            }
        }
        return batch;
    }

    private Enrollment upsertPendingEnrollment(Student student, Course course, Batch batch,
                                               TrainingMode mode, BigDecimal payable) {
        Enrollment enrollment = enrollmentRepository
                .findByStudent_IdAndCourse_IdAndStatus(student.getId(), course.getId(),
                        EnrollmentStatus.PENDING_PAYMENT)
                .orElseGet(() -> Enrollment.builder()
                        .student(student)
                        .course(course)
                        .status(EnrollmentStatus.PENDING_PAYMENT)
                        .build());
        enrollment.setBatch(batch);
        enrollment.setTrainingMode(mode);
        enrollment.setAmount(payable);
        enrollment.setPaymentStatus(PaymentStatus.CREATED);
        return enrollmentRepository.save(enrollment);
    }


    private String createRazorpayOrder(String receipt, long paise, String slug) {
        try {
            JSONObject request = new JSONObject();
            request.put("amount", paise);
            request.put("currency", CURRENCY);
            request.put("receipt", receipt);
            JSONObject notes = new JSONObject();
            notes.put("course", slug);
            request.put("notes", notes);
            Order order = razorpayClient.orders.create(request);
            return order.get("id");
        } catch (Exception ex) {
            log.error("Razorpay order creation failed", ex);
            throw new BusinessException("Could not start the payment. Please try again later.");
        }
    }

    // ---------------------------------------------------------------------
    // Signature verification + enrollment activation
    // ---------------------------------------------------------------------
    @Override
    @Transactional
    public PaymentDto verifyAndActivate(VerifyPaymentRequest request, User user) {
        if (request == null || isBlank(request.getRazorpayOrderId())
                || isBlank(request.getRazorpayPaymentId()) || isBlank(request.getRazorpaySignature())) {
            throw new BusinessException("Incomplete payment verification data.");
        }
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found."));

        if (user == null || !payment.getStudent().getUser().getId().equals(user.getId())) {
            throw new BusinessException("This payment does not belong to you.");
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return toDto(payment); // idempotent
        }

        String expected = hmacHex(
                request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId(),
                razorpayConfig.getKeySecret());

        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                request.getRazorpaySignature().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8))) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Signature verification failed.");
            paymentRepository.save(payment);
            markEnrollmentPaymentStatus(payment, PaymentStatus.FAILED);
            throw new BusinessException("Payment verification failed. You have not been charged for a failed verification.");
        }

        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
        activateEnrollment(payment);
        log.info("Payment verified and enrollment activated: receipt={}", payment.getReceiptNumber());
        return toDto(payment);
    }

    @Override
    @Transactional
    public PaymentDto confirmDemoPayment(String orderId, User user) {
        if (razorpayConfig.isEnabled()) {
            throw new BusinessException("Demo payment confirmation is not available while the live gateway is enabled.");
        }
        if (isBlank(orderId)) {
            throw new BusinessException("Missing payment reference.");
        }
        Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found."));
        if (user == null || !payment.getStudent().getUser().getId().equals(user.getId())) {
            throw new BusinessException("This payment does not belong to you.");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return toDto(payment); // idempotent
        }
        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new BusinessException("This payment can no longer be confirmed.");
        }
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        payment.setPaymentMethod("DEMO");
        Payment saved = paymentRepository.save(payment);
        activateEnrollment(saved);
        log.info("Demo payment confirmed and enrollment activated: receipt={}", saved.getReceiptNumber());
        return toDto(saved);
    }

    /**
     * Activates the seat reserved for a settled payment. Reuses the PENDING_PAYMENT
     * enrollment created at order time (linking batch, amount and mode) and only creates
     * a new ACTIVE enrollment if none was reserved.
     */
    private void activateEnrollment(Payment payment) {
        Long studentId = payment.getStudent().getId();
        Long courseId = payment.getCourse().getId();
        Enrollment enrollment = enrollmentRepository
                .findByStudent_IdAndCourse_IdAndStatus(studentId, courseId, EnrollmentStatus.PENDING_PAYMENT)
                .orElseGet(() -> enrollmentRepository
                        .findByStudent_IdAndCourse_Id(studentId, courseId).orElse(null));
        if (enrollment == null) {
            enrollment = Enrollment.builder()
                    .student(payment.getStudent())
                    .course(payment.getCourse())
                    .build();
        }
        Batch batch = payment.getBatch() != null ? payment.getBatch() : enrollment.getBatch();
        enrollment.setBatch(batch);
        enrollment.setPayment(payment);
        enrollment.setAmount(payment.getAmount());
        enrollment.setPaymentStatus(PaymentStatus.SUCCESS);
        enrollment.setTrainingMode(batch != null && batch.getMode() != null
                ? batch.getMode() : payment.getCourse().getTrainingMode());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        Enrollment activated = enrollmentRepository.save(enrollment);
        redeemCoupon(payment);

        // The seat is paid for and open: confirm the money first, then the enrollment.
        notificationService.paymentConfirmed(payment);
        notificationService.enrollmentConfirmed(activated);
    }

    /**
     * Records a coupon redemption. Called exactly once per payment at the moment it
     * transitions to SUCCESS, so the usage limit is enforced without double counting.
     */
    private void redeemCoupon(Payment payment) {
        Coupon coupon = payment.getCoupon();
        if (coupon != null) {
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
            log.info("Coupon {} redeemed ({} / {}).", coupon.getCode(), coupon.getUsedCount(),
                    coupon.getUsageLimit() == null ? "unlimited" : coupon.getUsageLimit());
        }
    }

    /** Reflects a failed payment on the reserved PENDING_PAYMENT enrollment (seat released). */
    private void markEnrollmentPaymentStatus(Payment payment, PaymentStatus status) {
        enrollmentRepository.findByStudent_IdAndCourse_IdAndStatus(
                        payment.getStudent().getId(), payment.getCourse().getId(), EnrollmentStatus.PENDING_PAYMENT)
                .ifPresent(enrollment -> {
                    enrollment.setPaymentStatus(status);
                    enrollmentRepository.save(enrollment);
                });
    }

    // ---------------------------------------------------------------------
    // Webhook handling
    // ---------------------------------------------------------------------
    @Override
    @Transactional
    public void handleWebhook(String payload, String signature) {
        if (isBlank(payload) || isBlank(signature)) {
            throw new BusinessException("Missing webhook payload or signature.");
        }
        String expected = hmacHex(payload, razorpayConfig.getWebhookSecret());
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8))) {
            log.warn("Rejected Razorpay webhook with invalid signature.");
            throw new BusinessException("Invalid webhook signature.");
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asText("");
            JsonNode paymentEntity = root.path("payload").path("payment").path("entity");

            switch (event) {
                case "payment.captured" -> handleCaptured(paymentEntity);
                case "payment.failed" -> handleFailed(paymentEntity, root);
                case "refund.created", "refund.processed" -> handleRefund(root);
                default -> log.debug("Ignoring unhandled Razorpay event: {}", event);
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to process Razorpay webhook", ex);
            throw new BusinessException("Could not process webhook.");
        }
    }

    private void handleCaptured(JsonNode entity) {
        String orderId = entity.path("order_id").asText(null);
        if (orderId == null) {
            return;
        }
        paymentRepository.findByRazorpayOrderId(orderId).ifPresent(payment -> {
            if (payment.getStatus() != PaymentStatus.SUCCESS) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setRazorpayPaymentId(entity.path("id").asText(payment.getRazorpayPaymentId()));
                payment.setPaymentMethod(entity.path("method").asText(null));
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);
                activateEnrollment(payment);
                log.info("Webhook confirmed payment: receipt={}", payment.getReceiptNumber());
            }
        });
    }

    private void handleFailed(JsonNode entity, JsonNode root) {
        String orderId = entity.path("order_id").asText(null);
        if (orderId == null) {
            return;
        }
        paymentRepository.findByRazorpayOrderId(orderId).ifPresent(payment -> {
            if (payment.getStatus() != PaymentStatus.SUCCESS) {
                payment.setStatus(PaymentStatus.FAILED);
                String reason = root.path("payload").path("error").path("description").asText("Payment failed.");
                payment.setFailureReason(reason);
                paymentRepository.save(payment);
                markEnrollmentPaymentStatus(payment, PaymentStatus.FAILED);
            }
        });
    }

    private void handleRefund(JsonNode root) {
        JsonNode refund = root.path("payload").path("refund").path("entity");
        String paymentId = refund.path("payment_id").asText(null);
        if (paymentId == null) {
            return;
        }
        paymentRepository.findByRazorpayPaymentId(paymentId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRazorpayRefundId(refund.path("id").asText(null));
            payment.setRefundedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            log.info("Webhook marked payment REFUNDED: receipt={}", payment.getReceiptNumber());
        });
    }

    // ---------------------------------------------------------------------
    // History / receipt / reports
    // ---------------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public List<PaymentDto> historyFor(User user) {
        Student student = studentRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new BusinessException("Student profile is missing."));
        return paymentRepository.findByStudent_IdOrderByCreatedAtDesc(student.getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDto receipt(Long paymentId, User user) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found."));
        ensureCanView(payment, user);
        return toDto(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDto findByReceiptNumber(String receiptNumber, User user) {
        Payment payment = paymentRepository.findByReceiptNumber(receiptNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found."));
        ensureCanView(payment, user);
        return toDto(payment);
    }

    private void ensureCanView(Payment payment, User user) {
        if (user == null) {
            throw new BusinessException("Please log in to view this receipt.");
        }
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (user.getRole() == Role.STUDENT
                && payment.getStudent().getUser().getId().equals(user.getId())) {
            return;
        }
        throw new BusinessException("You are not allowed to view this payment.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentDto> adminReport(PaymentStatus status) {
        List<Payment> payments = status == null
                ? paymentRepository.findAll().stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .collect(Collectors.toList())
                : paymentRepository.findByStatusOrderByCreatedAtDesc(status);
        return payments.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> adminSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        long total = paymentRepository.count();
        summary.put("total", total);
        for (PaymentStatus status : PaymentStatus.values()) {
            summary.put(status.name().toLowerCase(), paymentRepository.countByStatus(status));
        }
        BigDecimal collected = paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS);
        BigDecimal refunded = paymentRepository.sumAmountByStatus(PaymentStatus.REFUNDED);
        summary.put("collected", formatInr(collected));
        summary.put("refunded", formatInr(refunded));
        summary.put("net", formatInr(collected.subtract(refunded)));
        return summary;
    }

    // ---------------------------------------------------------------------
    // Mapping helpers
    // ---------------------------------------------------------------------
    private PaymentOrderDto toOrderDto(Payment payment, long paise, String orderId, boolean demo, boolean settled, Long enrollmentId) {
        User studentUser = payment.getStudent().getUser();
        return new PaymentOrderDto()
                .setId(payment.getId())
                .setRazorpayKeyId(razorpayConfig.getKeyId())
                .setOrderId(orderId)
                .setAmountInPaise(paise)
                .setCurrency(CURRENCY)
                .setReceiptNumber(payment.getReceiptNumber())
                .setAmount(payment.getAmount())
                .setCourseSlug(payment.getCourse().getSlug())
                .setBatchId(payment.getBatch() == null ? null : payment.getBatch().getId())
                .setEnrollmentId(enrollmentId)
                .setDemo(demo)
                .setSettled(settled)
                .setMerchantName(MERCHANT_NAME)
                .setCourseTitle(payment.getCourse().getTitle())
                .setCustomerName(fullName(studentUser))
                .setCustomerEmail(studentUser.getEmail())
                .setCustomerPhone(studentUser.getPhone() == null ? "" : studentUser.getPhone());
    }

    private PaymentDto toDto(Payment payment) {
        Course course = payment.getCourse();
        User studentUser = payment.getStudent().getUser();
        PaymentDto dto = new PaymentDto()
                .setId(payment.getId())
                .setReceiptNumber(payment.getReceiptNumber())
                .setRazorpayOrderId(payment.getRazorpayOrderId())
                .setRazorpayPaymentId(payment.getRazorpayPaymentId())
                .setCourseId(course.getId())
                .setCourseTitle(course.getTitle())
                .setCourseSlug(course.getSlug())
                .setStudentId(payment.getStudent().getId())
                .setStudentName(fullName(studentUser))
                .setStudentEmail(studentUser.getEmail())
                .setAmount(payment.getAmount())
                .setBaseAmount(payment.getBaseAmount())
                .setDiscountAmount(payment.getDiscountAmount())
                .setCurrency(payment.getCurrency())
                .setStatus(payment.getStatus())
                .setStatusLabel(capitalize(payment.getStatus()))
                .setPaymentMethod(payment.getPaymentMethod())
                .setFailureReason(payment.getFailureReason())
                .setCreatedAt(payment.getCreatedAt())
                .setPaidAt(payment.getPaidAt())
                .setRefundedAt(payment.getRefundedAt())
                .setFormattedAmount(formatInr(payment.getAmount()))
                .setFormattedBaseAmount(formatInr(payment.getBaseAmount()))
                .setFormattedDiscount(formatInr(payment.getDiscountAmount()));
        if (payment.getCoupon() != null) {
            dto.setCouponCode(payment.getCoupon().getCode());
        }
        return dto;
    }

    private Coupon resolveCoupon(String code, Long courseId) {
        if (isBlank(code)) {
            return null;
        }
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BusinessException("Invalid coupon code."));
        String reason = coupon.rejectionReason(courseId);
        if (reason != null) {
            throw new BusinessException(reason);
        }
        return coupon;
    }

    private String generateReceiptNumber() {
        return "FBT-" + LocalDateTime.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String hmacHex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : raw) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            log.error("HMAC computation failed", ex);
            throw new BusinessException("Payment security check failed.");
        }
    }

    private String fullName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }

    private String trainerName(Course course) {
        Trainer trainer = course.getTrainer();
        return trainer == null || trainer.getUser() == null ? null : fullName(trainer.getUser());
    }

    private String modeLabel(TrainingMode mode) {
        if (mode == null) {
            return "";
        }
        String name = mode.name();
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }

    private String batchMeta(Batch batch) {
        StringBuilder sb = new StringBuilder();
        if (batch.getStartDate() != null) {
            sb.append("Starts ").append(batch.getStartDate().format(BATCH_DATE));
        }
        if (batch.getMaxSeats() != null) {
            long holding = enrollmentRepository.countSeatsHeldByBatch(batch.getId());
            long left = Math.max(0, batch.getMaxSeats() - holding);
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(left).append(left == 1 ? " seat left" : " seats left");
        }
        return sb.toString();
    }

    private String capitalize(PaymentStatus status) {
        if (status == null) {
            return "";
        }
        String name = status.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private String formatInr(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        format.setMaximumFractionDigits(2);
        return format.format(amount);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
