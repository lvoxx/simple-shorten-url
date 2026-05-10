package io.lvoxx.ssurl.common.exception;

public class UnauthorizedException extends AppException {
    public UnauthorizedException(String reason) {
        super("Unauthorized: " + reason, reason);
    }
}
