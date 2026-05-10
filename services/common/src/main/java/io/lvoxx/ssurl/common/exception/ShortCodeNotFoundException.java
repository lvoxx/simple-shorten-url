package io.lvoxx.ssurl.common.exception;

public class ShortCodeNotFoundException extends RuntimeException {
    public ShortCodeNotFoundException(String code) {
        super("Short code not found: " + code);
    }
}
