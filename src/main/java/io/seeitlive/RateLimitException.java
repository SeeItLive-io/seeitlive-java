package io.seeitlive;

/**
 * Thrown for HTTP 429 responses. {@link #getRetryAfter()} is {@code null} when the response
 * carried no (parseable) {@code Retry-After} header — a legitimate {@code Retry-After: 0}
 * ("retry now") is preserved as {@code 0}, not coerced to absent.
 */
public class RateLimitException extends SeeItLiveException {

    private final Integer retryAfter;

    public RateLimitException(String message, Integer retryAfter, String code) {
        super(429, message == null ? "Rate limited" : message, code);
        this.retryAfter = retryAfter;
    }

    public Integer getRetryAfter() {
        return retryAfter;
    }
}
