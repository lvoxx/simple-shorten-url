package io.lvoxx.ssurl.api_service.exception;

import io.lvoxx.ssurl.common.exception.AppException;
import io.lvoxx.ssurl.common.exception.DomainBlacklistedException;
import io.lvoxx.ssurl.common.exception.RateLimitExceededException;
import io.lvoxx.ssurl.common.exception.ShortCodeNotFoundException;
import io.lvoxx.ssurl.common.exception.UnauthorizedException;
import io.lvoxx.ssurl.common.exception.UrlExpiredException;
import io.lvoxx.ssurl.common.exception.UrlNotFoundException;
import io.lvoxx.ssurl.common.exception.UserAlreadyExistsException;
import io.lvoxx.ssurl.common.exception.UserNotFoundException;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(ShortCodeNotFoundException.class)
    public ProblemDetail handleShortCodeNotFound(ShortCodeNotFoundException ex, Locale locale) {
        ProblemDetail pd = ProblemDetail.forStatus(404);
        pd.setTitle("Short Code Not Found");
        pd.setDetail(resolveMessage("error.shortcode.notfound", ex, locale));
        return pd;
    }

    @ExceptionHandler(UrlExpiredException.class)
    public ProblemDetail handleUrlExpired(UrlExpiredException ex, Locale locale) {
        ProblemDetail pd = ProblemDetail.forStatus(410);
        pd.setTitle("URL Expired");
        pd.setDetail(resolveMessage("error.shortcode.expired", ex, locale));
        return pd;
    }

    @ExceptionHandler(DomainBlacklistedException.class)
    public ProblemDetail handleDomainBlacklisted(DomainBlacklistedException ex, Locale locale) {
        ProblemDetail pd = ProblemDetail.forStatus(422);
        pd.setTitle("Domain Blacklisted");
        pd.setDetail(resolveMessage("error.domain.blacklisted", ex, locale));
        return pd;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex, Locale locale) {
        ProblemDetail pd = ProblemDetail.forStatus(404);
        pd.setTitle("User Not Found");
        pd.setDetail(resolveMessage("error.user.notfound", ex, locale));
        return pd;
    }

    @ExceptionHandler(UrlNotFoundException.class)
    public ProblemDetail handleUrlNotFound(UrlNotFoundException ex, Locale locale) {
        ProblemDetail pd = ProblemDetail.forStatus(404);
        pd.setTitle("URL Not Found");
        pd.setDetail(resolveMessage("error.url.notfound", ex, locale));
        return pd;
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException ex, Locale locale) {
        ProblemDetail pd = ProblemDetail.forStatus(409);
        pd.setTitle("User Already Exists");
        pd.setDetail(resolveMessage("error.user.exists", ex, locale));
        return pd;
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ProblemDetail handleRateLimitExceeded(RateLimitExceededException ex, Locale locale) {
        ProblemDetail pd = ProblemDetail.forStatus(429);
        pd.setTitle("Rate Limit Exceeded");
        pd.setDetail(resolveMessage("error.ratelimit.exceeded", ex, locale));
        return pd;
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ProblemDetail handleUnauthorized(UnauthorizedException ex, Locale locale) {
        ProblemDetail pd = ProblemDetail.forStatus(401);
        pd.setTitle("Unauthorized");
        pd.setDetail(resolveMessage("error.unauthorized", ex, locale));
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
