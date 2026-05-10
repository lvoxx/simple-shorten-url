package io.lvoxx.ssurl.common.exception;

public class RateLimitExceededException extends AppException {
    public RateLimitExceededException(long retryAfterSeconds) {
        super("Rate limit exceeded. Retry after " + retryAfterSeconds + " seconds", retryAfterSeconds);
    }
}
