package com.futureboundtech.controller;

import com.futureboundtech.dto.CouponDto;
import com.futureboundtech.enums.PaymentStatus;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.service.AdminService;
import com.futureboundtech.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminCommerceController {

    private final AdminService adminService;
    private final PaymentService paymentService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, true));
        binder.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, true));
    }

    // -----------------------------------------------------------------
    // Payments
    // -----------------------------------------------------------------
    @GetMapping("/payments")
    public String payments(@RequestParam(value = "status", required = false) PaymentStatus status,
                           Model model) {
        model.addAttribute("pageTitle", "Payments — Admin");
        model.addAttribute("payments", paymentService.adminReport(status));
        model.addAttribute("summary", paymentService.adminSummary());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", PaymentStatus.values());
        return "admin/payments";
    }

    // -----------------------------------------------------------------
    // Coupons
    // -----------------------------------------------------------------
    @GetMapping("/coupons")
    public String coupons(Model model) {
        model.addAttribute("pageTitle", "Coupons — Admin");
        model.addAttribute("coupons", adminService.listCoupons());
        return "admin/coupons";
    }

    @GetMapping("/coupons/new")
    public String newCouponForm(Model model) {
        populateCouponForm(model, new CouponDto(), false, null);
        return "admin/coupon-form";
    }

    @PostMapping("/coupons")
    public String createCoupon(@Valid @ModelAttribute("couponDto") CouponDto dto,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateCouponForm(model, dto, false, null);
            model.addAttribute("errorMessage", "Please review the highlighted fields.");
            return "admin/coupon-form";
        }
        try {
            adminService.createCoupon(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Coupon created.");
            return "redirect:/admin/coupons";
        } catch (BusinessException ex) {
            populateCouponForm(model, dto, false, null);
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/coupon-form";
        }
    }

    @GetMapping("/coupons/{id}/edit")
    public String editCouponForm(@PathVariable Long id, Model model) {
        populateCouponForm(model, adminService.getCoupon(id), true, id);
        return "admin/coupon-form";
    }

    @PostMapping("/coupons/{id}")
    public String updateCoupon(@PathVariable Long id,
                               @Valid @ModelAttribute("couponDto") CouponDto dto,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateCouponForm(model, dto, true, id);
            model.addAttribute("errorMessage", "Please review the highlighted fields.");
            return "admin/coupon-form";
        }
        try {
            adminService.updateCoupon(id, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Coupon updated.");
            return "redirect:/admin/coupons";
        } catch (BusinessException ex) {
            populateCouponForm(model, dto, true, id);
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/coupon-form";
        }
    }

    @PostMapping("/coupons/{id}/delete")
    public String deleteCoupon(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteCoupon(id);
            redirectAttributes.addFlashAttribute("successMessage", "Coupon deleted.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/coupons";
    }

    private void populateCouponForm(Model model, CouponDto dto, boolean editing, Long id) {
        model.addAttribute("pageTitle", (editing ? "Edit Coupon" : "New Coupon") + " — Admin");
        model.addAttribute("couponDto", dto);
        model.addAttribute("editing", editing);
        model.addAttribute("couponId", id);
        model.addAttribute("discountTypes", com.futureboundtech.enums.CouponDiscountType.values());
        model.addAttribute("allCourses", adminService.listCourseOptions());
        model.addAttribute("formAction", editing ? "/admin/coupons/" + id : "/admin/coupons");
    }
}
