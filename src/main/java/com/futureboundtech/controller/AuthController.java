package com.futureboundtech.controller;

import com.futureboundtech.dto.PasswordResetDto;
import com.futureboundtech.dto.UserRegistrationDto;
import com.futureboundtech.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("pageTitle", "Login — Future Bound Tech");
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("pageTitle", "Register — Future Bound Tech");
        model.addAttribute("registrationDto", new UserRegistrationDto());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registrationDto") UserRegistrationDto registrationDto,
                               BindingResult result,
                               Model model) {
        if (result.hasErrors()) {
            return "auth/register";
        }
        try {
            authService.registerStudent(registrationDto);
            return "redirect:/login?registered=true";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        model.addAttribute("pageTitle", "Forgot Password — Future Bound Tech");
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, Model model) {
        try {
            authService.createPasswordResetTokenForUser(email);
            model.addAttribute("message", "We have sent a reset password link to your email.");
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam(value = "token", required = false) String token, Model model) {
        if (token == null) {
            return "redirect:/login?error=InvalidToken";
        }
        PasswordResetDto dto = new PasswordResetDto();
        dto.setToken(token);
        model.addAttribute("resetDto", dto);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@Valid @ModelAttribute("resetDto") PasswordResetDto resetDto,
                                       BindingResult result,
                                       Model model) {
        if (result.hasErrors()) {
            return "auth/reset-password";
        }
        try {
            authService.resetPassword(resetDto);
            return "redirect:/login?reset=true";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/reset-password";
        }
    }
}
