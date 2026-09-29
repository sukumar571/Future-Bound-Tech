package com.futureboundtech.api;

import com.futureboundtech.dto.api.ApiResponse;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.exception.RateLimitedException;
import com.futureboundtech.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Locale;

/**
 * JSON error handling scoped to the {@code /api} controllers only, so the REST
 * layer returns machine-readable {@link ApiResponse} bodies with correct HTTP
 * status codes while the Thymeleaf MVC {@code GlobalExceptionHandler} keeps
 * rendering HTML error pages.
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.futureboundtech.api")
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> business(BusinessException ex) {
        String message = ex.getMessage() == null ? "Request could not be processed." : ex.getMessage();
        HttpStatus status = isConflict(message) ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(ApiResponse.error(message));
    }

    @ExceptionHandler(RateLimitedException.class)
    public ResponseEntity<ApiResponse<Void>> rateLimited(RateLimitedException ex) {
        String message = ex.getMessage() == null ? "Too many requests. Please slow down."
                : ex.getMessage();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ApiResponse.error(message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException ex) {
        List<ApiResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed", errors));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> accessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("You do not have permission to perform this action."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> unauthorized(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid email or password."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> conflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("The submitted details conflict with an existing record."));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiResponse<Void>> badRequest(Exception ex) {
        log.debug("Rejected API request: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error("Malformed or incomplete request body."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unexpected(Exception ex) {
        log.error("Unhandled API error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again."));
    }

    private ApiResponse.FieldError toFieldError(FieldError error) {
        return new ApiResponse.FieldError(error.getField(), error.getDefaultMessage());
    }

    private boolean isConflict(String message) {
        String m = message.toLowerCase(Locale.ROOT);
        return m.contains("already") || m.contains("exist") || m.contains("duplicate") || m.contains("conflict");
    }
}
