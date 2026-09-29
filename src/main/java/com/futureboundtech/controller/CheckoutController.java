package com.futureboundtech.controller;

import com.futureboundtech.dto.EnrollmentQuoteDto;
import com.futureboundtech.dto.PaymentDto;
import com.futureboundtech.dto.PaymentOrderDto;
import com.futureboundtech.dto.StudentEnrollmentDto;
import com.futureboundtech.dto.VerifyPaymentRequest;
import com.futureboundtech.entity.User;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.exception.ResourceNotFoundException;
import com.futureboundtech.security.CustomUserDetails;
import com.futureboundtech.service.CourseService;
import com.futureboundtech.service.PaymentService;
import com.futureboundtech.service.QrCodeService;
import com.futureboundtech.service.StudentDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Student enrollment checkout: a server-driven quote (course, batch, mode, fee,
 * coupon and final amount) followed by payment. An enrollment is only ever
 * activated after the payment is settled on the server — never from a frontend
 * "success" signal.
 */
@Slf4j
@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class CheckoutController {

    private final PaymentService paymentService;
    private final CourseService courseService;
    private final StudentDashboardService studentDashboardService;
    private final QrCodeService qrCodeService;

    /** Demo-only UPI handle shown on the placeholder-payment QR. Never used for real money. */
    private static final String DEMO_UPI_ID = "futureboundtech@demo";

    @GetMapping("/checkout")
    public String checkout(@RequestParam("slug") String slug,
                           @RequestParam(value = "batchId", required = false) Long batchId,
                           @RequestParam(value = "coupon", required = false) String coupon,
                           @AuthenticationPrincipal CustomUserDetails principal,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        User user = principal.getUser();
        EnrollmentQuoteDto quote;
        try {
            quote = paymentService.quote(slug, batchId, coupon, user);
        } catch (BusinessException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/courses/" + slug;
        }
        model.addAttribute("pageTitle", "Checkout — Future Bound Tech");
        model.addAttribute("quote", quote);
        model.addAttribute("enrollableBatches", courseService.findEnrollableBatches(slug));
        return "student/checkout";
    }

    private String buildDemoUpiUri(java.math.BigDecimal amount, String courseTitle) {
        String value = amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        String payee = java.net.URLEncoder.encode("Future Bound Tech", java.nio.charset.StandardCharsets.UTF_8);
        String note = java.net.URLEncoder.encode(courseTitle == null ? "Course enrollment" : courseTitle,
                java.nio.charset.StandardCharsets.UTF_8);
        return "upi://pay?pa=" + DEMO_UPI_ID + "&pn=" + payee + "&am=" + value + "&cu=INR&tn=" + note;
    }

    @PostMapping("/checkout/coupon")
    public String applyCoupon(@RequestParam("slug") String slug,
                              @RequestParam(value = "batchId", required = false) Long batchId,
                              @RequestParam(value = "coupon", required = false) String coupon) {
        StringBuilder url = new StringBuilder("redirect:/student/checkout?slug=").append(slug);
        if (batchId != null) {
            url.append("&batchId=").append(batchId);
        }
        if (coupon != null && !coupon.isBlank()) {
            url.append("&coupon=").append(coupon.trim());
        }
        return url.toString();
    }

    @PostMapping("/checkout/pay")
    public String pay(@RequestParam("slug") String slug,
                      @RequestParam(value = "batchId", required = false) Long batchId,
                      @RequestParam(value = "coupon", required = false) String coupon,
                      @AuthenticationPrincipal CustomUserDetails principal,
                      Model model,
                      RedirectAttributes redirectAttributes) {
        User user = principal.getUser();
        PaymentOrderDto order;
        try {
            order = paymentService.createOrder(slug, batchId, user, coupon);
        } catch (BusinessException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            StringBuilder url = new StringBuilder("redirect:/student/checkout?slug=").append(slug);
            if (batchId != null) {
                url.append("&batchId=").append(batchId);
            }
            if (coupon != null && !coupon.isBlank()) {
                url.append("&coupon=").append(coupon.trim());
            }
            return url.toString();
        }

        if (order.isSettled()) {
            // Free seat or already settled server-side: enrollment is active.
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment received — your enrollment is now active!");
            return "redirect:/student/enrollment/confirmation?courseSlug=" + order.getCourseSlug();
        }

        model.addAttribute("pageTitle", "Complete payment — Future Bound Tech");
        model.addAttribute("order", order);

        if (order.isDemo()) {
            // Demo mode with a payable amount: show a scannable placeholder UPI QR.
            // Enrollment is activated only when the student confirms below.
            model.addAttribute("demoUpiId", DEMO_UPI_ID);
            model.addAttribute("demoUpiQr",
                    qrCodeService.toPngDataUri(buildDemoUpiUri(order.getAmount(), order.getCourseTitle()), 240));
            return "student/checkout-demo";
        }
        return "student/checkout-gateway";
    }

    /**
     * Demo payment confirmation: the student says they have paid via the placeholder QR.
     * The enrollment is activated server-side (ownership-checked, idempotent) — never from
     * the browser alone.
     */
    @PostMapping("/checkout/demo-confirm")
    public String demoConfirm(@RequestParam("orderId") String orderId,
                              @RequestParam("slug") String slug,
                              @AuthenticationPrincipal CustomUserDetails principal,
                              RedirectAttributes redirectAttributes) {
        try {
            PaymentDto dto = paymentService.confirmDemoPayment(orderId, principal.getUser());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment received — your enrollment is now active!");
            return "redirect:/student/enrollment/confirmation?courseSlug=" + dto.getCourseSlug();
        } catch (BusinessException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/student/checkout?slug=" + slug;
        }
    }

    /** Server-side signature verification. Enrollment activates here, not on the client. */
    @PostMapping(value = "/checkout/verify", produces = "application/json")
    @ResponseBody
    public Map<String, Object> verify(@RequestBody VerifyPaymentRequest request,
                                      @AuthenticationPrincipal CustomUserDetails principal) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            PaymentDto dto = paymentService.verifyAndActivate(request, principal.getUser());
            response.put("success", true);
            response.put("courseSlug", dto.getCourseSlug());
            response.put("redirect", "/student/enrollment/confirmation?courseSlug=" + dto.getCourseSlug());
        } catch (BusinessException | ResourceNotFoundException ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            response.put("redirect", "/student/payment/failed?reason="
                    + java.net.URLEncoder.encode(ex.getMessage(), java.nio.charset.StandardCharsets.UTF_8));
        }
        return response;
    }

    @GetMapping("/enrollment/confirmation")
    public String confirmation(@RequestParam("courseSlug") String courseSlug,
                              @AuthenticationPrincipal CustomUserDetails principal,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        User user = principal.getUser();
        List<StudentEnrollmentDto> history = studentDashboardService.getEnrollmentHistory(user);
        StudentEnrollmentDto enrollment = history.stream()
                .filter(e -> courseSlug.equals(e.getCourseSlug()))
                .findFirst()
                .orElse(null);
        if (enrollment == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "We could not find that enrollment.");
            return "redirect:/student/enrollments";
        }
        model.addAttribute("pageTitle", "Enrollment confirmed — Future Bound Tech");
        model.addAttribute("enrollment", enrollment);
        return "student/enrollment-confirmation";
    }

    @GetMapping("/enrollments")
    public String enrollments(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pageTitle", "My Enrollments — Future Bound Tech");
        model.addAttribute("enrollments", studentDashboardService.getEnrollmentHistory(principal.getUser()));
        return "student/enrollments";
    }

    /** Printable payment receipt. Ownership is enforced inside the service. */
    @GetMapping("/payments/{id}/receipt")
    public String receipt(@PathVariable Long id,
                          @AuthenticationPrincipal CustomUserDetails principal,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("pageTitle", "Receipt — Future Bound Tech");
            model.addAttribute("payment", paymentService.receipt(id, principal.getUser()));
            return "student/receipt";
        } catch (BusinessException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/student/payments";
        }
    }

    /** Shown when a checkout is cancelled or a payment could not be verified. */
    @GetMapping("/payment/failed")
    public String paymentFailed(@RequestParam(value = "reason", required = false) String reason,
                                @RequestParam(value = "slug", required = false) String slug,
                                Model model) {
        model.addAttribute("pageTitle", "Payment incomplete — Future Bound Tech");
        model.addAttribute("reason", reason);
        model.addAttribute("courseSlug", slug);
        return "student/payment-failed";
    }
}
