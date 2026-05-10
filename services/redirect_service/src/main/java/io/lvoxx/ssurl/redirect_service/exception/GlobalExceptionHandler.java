package io.lvoxx.ssurl.redirect_service.exception;

import io.lvoxx.ssurl.common.exception.AppException;
import io.lvoxx.ssurl.common.exception.ShortCodeNotFoundException;
import io.lvoxx.ssurl.common.exception.UrlExpiredException;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(ShortCodeNotFoundException.class)
    public ProblemDetail handleNotFound(ShortCodeNotFoundException ex, Locale locale) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setDetail(resolveMessage("error.shortcode.notfound", ex, locale));
        return pd;
    }

    @ExceptionHandler(UrlExpiredException.class)
    public ProblemDetail handleExpired(UrlExpiredException ex, Locale locale) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.GONE);
        pd.setDetail(resolveMessage("error.shortcode.expired", ex, locale));
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
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
