package io.lvoxx.ssurl.common.exception;

public class UrlNotFoundException extends RuntimeException {
    public UrlNotFoundException(Long id) {
        super("URL not found: id=" + id);
    }
}
