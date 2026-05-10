package io.lvoxx.ssurl.common.exception;

public class UserAlreadyExistsException extends AppException {
    public UserAlreadyExistsException(String identifier) {
        super("User already exists: " + identifier, identifier);
    }
}
