package com.futureboundtech.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("pageTitle", "Not Found — Future Bound Tech");
        model.addAttribute("errorCode", "404");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(BusinessException ex,
                                          HttpServletRequest request,
                                          RedirectAttributes redirectAttributes,
                                          Model model) {
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:" + referer;
        }
        model.addAttribute("pageTitle", "Something went wrong — Future Bound Tech");
        model.addAttribute("errorCode", "400");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                               HttpServletRequest request,
                                               RedirectAttributes redirectAttributes,
                                               Model model) {
        String message = "The submitted details conflict with an existing record "
                + "(for example, an email or phone number that is already registered).";
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", message);
            return "redirect:" + referer;
        }
        model.addAttribute("pageTitle", "Duplicate entry — Future Bound Tech");
        model.addAttribute("errorCode", "409");
        model.addAttribute("errorMessage", message);
        return "error/error";
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        model.addAttribute("pageTitle", "Access denied — Future Bound Tech");
        model.addAttribute("errorCode", "403");
        model.addAttribute("errorMessage", ex.getMessage() == null
                ? "You do not have permission to perform this action." : ex.getMessage());
        return "error/access-denied";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUpload(MaxUploadSizeExceededException ex,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        String message = "The uploaded file is too large. Maximum size is 10 MB.";
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", message);
            return "redirect:" + referer;
        }
        model.addAttribute("pageTitle", "Upload too large — Future Bound Tech");
        model.addAttribute("errorCode", "413");
        model.addAttribute("errorMessage", message);
        return "error/error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGlobalException(Exception ex, Model model) {
        model.addAttribute("pageTitle", "Error — Future Bound Tech");
        model.addAttribute("errorCode", "500");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/error";
    }
}
