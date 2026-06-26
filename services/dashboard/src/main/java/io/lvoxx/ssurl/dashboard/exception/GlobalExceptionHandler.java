package io.lvoxx.ssurl.dashboard.exception;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import io.lvoxx.ssurl.common.exception.AppException;
import io.lvoxx.ssurl.common.exception.ShortCodeNotFoundException;
import io.lvoxx.ssurl.common.exception.UnauthorizedException;
import io.lvoxx.ssurl.common.util.Constants;

/** Maps dashboard exceptions to RFC-7807 {@link ProblemDetail} responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(ShortCodeNotFoundException.class)
    public ProblemDetail handleNotFound(ShortCodeNotFoundException ex, Locale locale) {
        ProblemDetail pd = ProblemDetail.forStatus(404);
        pd.setTitle("Link Not Found");
        pd.setDetail(resolveMessage(Constants.Messages.SHORTCODE_NOT_FOUND, ex, locale));
        return pd;
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ProblemDetail handleUnauthorized(UnauthorizedException ex, Locale locale) {
        ProblemDetail pd = ProblemDetail.forStatus(401);
        pd.setTitle("Unauthorized");
        pd.setDetail(resolveMessage(Constants.Messages.UNAUTHORIZED, ex, locale));
        return pd;
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ProblemDetail handleValidation(WebExchangeBindException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(400);
        pd.setTitle("Validation Failed");
        pd.setDetail("Request validation failed");
        pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail pd = ProblemDetail.forStatus(500);
        pd.setTitle("Internal Server Error");
        pd.setDetail("An unexpected error occurred");
        return pd;
    }

    private String resolveMessage(String code, AppException ex, Locale locale) {
        try {
            return messageSource.getMessage(code, ex.getArgs(), locale);
        } catch (Exception e) {
            return ex.getMessage();
        }
    }
}
