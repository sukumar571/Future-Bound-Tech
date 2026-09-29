package com.futureboundtech.exception;

/**
 * Raised when a caller exceeds an allowed request budget (e.g. the Future Mentor
 * rate limit). Mapped to HTTP 429 Too Many Requests on the JSON API.
 */
public class RateLimitedException extends RuntimeException {

    public RateLimitedException(String message) {
        super(message);
    }
}
