package io.lvoxx.ssurl.common.exception;

public class UrlNotFoundException extends AppException {
    public UrlNotFoundException(Long id) {
        super("URL not found: id=" + id, id);
    }
}
