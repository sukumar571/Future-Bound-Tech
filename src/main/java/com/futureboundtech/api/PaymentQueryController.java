package com.futureboundtech.api;

import com.futureboundtech.dto.PaymentDto;
import com.futureboundtech.dto.api.ApiResponse;
import com.futureboundtech.dto.api.PageDto;
import com.futureboundtech.enums.PaymentStatus;
import com.futureboundtech.security.CustomUserDetails;
import com.futureboundtech.service.PaymentService;
import com.futureboundtech.util.ApiPaging;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only payment listings for the REST layer. The Razorpay create/verify/webhook
 * flow lives in {@code PaymentApiController} and is intentionally left untouched.
 */
@RestController
@RequiredArgsConstructor
public class PaymentQueryController {

    private final PaymentService paymentService;

    @GetMapping("/api/student/payments")
    public ApiResponse<PageDto<PaymentDto>> myPayments(@AuthenticationPrincipal CustomUserDetails principal,
                                                       @PageableDefault(size = 20) Pageable pageable) {
        List<PaymentDto> payments = paymentService.historyFor(principal.getUser());
        return ApiResponse.ok(ApiPaging.paginate(payments, pageable));
    }

    @GetMapping("/api/admin/payments")
    public ApiResponse<PageDto<PaymentDto>> adminPayments(@RequestParam(value = "status", required = false) PaymentStatus status,
                                                          @PageableDefault(size = 20) Pageable pageable) {
        List<PaymentDto> payments = paymentService.adminReport(status);
        return ApiResponse.ok(ApiPaging.paginate(payments, pageable));
    }
}
