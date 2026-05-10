package io.lvoxx.ssurl.common.exception;

public abstract class AppException extends RuntimeException {

    private final Object[] args;

    protected AppException(String message, Object... args) {
        super(message);
        this.args = args;
    }

    public Object[] getArgs() {
        return args;
    }
}
