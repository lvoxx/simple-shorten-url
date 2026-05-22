package io.lvoxx.ssurl.common.exception;

public class IP2LocationFileReadException extends AppException {
    public IP2LocationFileReadException() {
        super("Failed to resolve IP location");
    }
}
