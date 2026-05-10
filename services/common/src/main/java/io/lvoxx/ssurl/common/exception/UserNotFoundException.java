package io.lvoxx.ssurl.common.exception;

public class UserNotFoundException extends AppException {
    public UserNotFoundException(String identifier) {
        super("User not found: " + identifier, identifier);
    }
}
