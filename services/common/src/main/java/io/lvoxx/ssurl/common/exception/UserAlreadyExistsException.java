package io.lvoxx.ssurl.common.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String identifier) {
        super("User already exists: " + identifier);
    }
}
