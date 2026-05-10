package io.lvoxx.ssurl.common.exception;

public class UrlExpiredException extends AppException {
    public UrlExpiredException(String code) {
        super("URL has expired: " + code, code);
    }
}
