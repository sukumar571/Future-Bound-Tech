package com.futureboundtech.dto.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * The single, consistent JSON envelope returned by every {@code /api/**} endpoint.
 *
 * <p>Success responses carry {@code data} (and an optional {@code message});
 * validation failures additionally populate {@code errors}. Null members are
 * omitted so a plain {@code {success, data}} body stays compact.</p>
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private List<FieldError> errors;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<T>().setSuccess(true).setData(data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<T>().setSuccess(true).setMessage(message).setData(data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<T>().setSuccess(true).setMessage("Created").setData(data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<T>().setSuccess(false).setMessage(message);
    }

    public static <T> ApiResponse<T> error(String message, List<FieldError> errors) {
        return new ApiResponse<T>().setSuccess(false).setMessage(message).setErrors(errors);
    }

    /** A single field-level validation error surfaced in the {@code errors} array. */
    @Data
    @Accessors(chain = true)
    public static class FieldError {
        private String field;
        private String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }
}
