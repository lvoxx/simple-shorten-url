package io.lvoxx.ssurl.api_service.exception;

import io.lvoxx.ssurl.common.exception.DomainBlacklistedException;
import io.lvoxx.ssurl.common.exception.RateLimitExceededException;
import io.lvoxx.ssurl.common.exception.ShortCodeNotFoundException;
import io.lvoxx.ssurl.common.exception.UnauthorizedException;
import io.lvoxx.ssurl.common.exception.UrlExpiredException;
import io.lvoxx.ssurl.common.exception.UrlNotFoundException;
import io.lvoxx.ssurl.common.exception.UserAlreadyExistsException;
import io.lvoxx.ssurl.common.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock private MessageSource messageSource;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(messageSource);
    }

    @Test
    @DisplayName("ShortCodeNotFoundException -> 404")
    void handleShortCodeNotFound() {
        when(messageSource.getMessage(eq("error.shortcode.notfound"), any(), any(Locale.class)))
                .thenReturn("Short code 'abc' does not exist");

        ProblemDetail pd = handler.handleShortCodeNotFound(
                new ShortCodeNotFoundException("abc"), Locale.ENGLISH);

        assertThat(pd.getStatus()).isEqualTo(404);
        assertThat(pd.getTitle()).isEqualTo("Short Code Not Found");
        assertThat(pd.getDetail()).isEqualTo("Short code 'abc' does not exist");
    }

    @Test
    @DisplayName("UrlExpiredException -> 410")
    void handleUrlExpired() {
        when(messageSource.getMessage(eq("error.shortcode.expired"), any(), any(Locale.class)))
                .thenReturn("Short code 'abc' has expired");

        ProblemDetail pd = handler.handleUrlExpired(
                new UrlExpiredException("abc"), Locale.ENGLISH);

        assertThat(pd.getStatus()).isEqualTo(410);
        assertThat(pd.getTitle()).isEqualTo("URL Expired");
    }

    @Test
    @DisplayName("DomainBlacklistedException -> 422")
    void handleDomainBlacklisted() {
        when(messageSource.getMessage(eq("error.domain.blacklisted"), any(), any(Locale.class)))
                .thenReturn("The domain is not allowed");

        ProblemDetail pd = handler.handleDomainBlacklisted(
                new DomainBlacklistedException("spam.com"), Locale.ENGLISH);

        assertThat(pd.getStatus()).isEqualTo(422);
        assertThat(pd.getTitle()).isEqualTo("Domain Blacklisted");
    }

    @Test
    @DisplayName("UserNotFoundException -> 404")
    void handleUserNotFound() {
        when(messageSource.getMessage(eq("error.user.notfound"), any(), any(Locale.class)))
                .thenReturn("User not found");

        ProblemDetail pd = handler.handleUserNotFound(
                new UserNotFoundException("alice"), Locale.ENGLISH);

        assertThat(pd.getStatus()).isEqualTo(404);
        assertThat(pd.getTitle()).isEqualTo("User Not Found");
    }

    @Test
    @DisplayName("UrlNotFoundException -> 404")
    void handleUrlNotFound() {
        when(messageSource.getMessage(eq("error.url.notfound"), any(), any(Locale.class)))
                .thenReturn("URL not found");

        ProblemDetail pd = handler.handleUrlNotFound(
                new UrlNotFoundException(1L), Locale.ENGLISH);

        assertThat(pd.getStatus()).isEqualTo(404);
        assertThat(pd.getTitle()).isEqualTo("URL Not Found");
    }

    @Test
    @DisplayName("UserAlreadyExistsException -> 409")
    void handleUserAlreadyExists() {
        when(messageSource.getMessage(eq("error.user.exists"), any(), any(Locale.class)))
                .thenReturn("User already exists");

        ProblemDetail pd = handler.handleUserAlreadyExists(
                new UserAlreadyExistsException("alice"), Locale.ENGLISH);

        assertThat(pd.getStatus()).isEqualTo(409);
        assertThat(pd.getTitle()).isEqualTo("User Already Exists");
    }

    @Test
    @DisplayName("RateLimitExceededException -> 429")
    void handleRateLimitExceeded() {
        when(messageSource.getMessage(eq("error.ratelimit.exceeded"), any(), any(Locale.class)))
                .thenReturn("Too many requests");

        ProblemDetail pd = handler.handleRateLimitExceeded(
                new RateLimitExceededException(30L), Locale.ENGLISH);

        assertThat(pd.getStatus()).isEqualTo(429);
        assertThat(pd.getTitle()).isEqualTo("Rate Limit Exceeded");
    }

    @Test
    @DisplayName("UnauthorizedException -> 401")
    void handleUnauthorized() {
        when(messageSource.getMessage(eq("error.unauthorized"), any(), any(Locale.class)))
                .thenReturn("Unauthorized");

        ProblemDetail pd = handler.handleUnauthorized(
                new UnauthorizedException("Invalid credentials"), Locale.ENGLISH);

        assertThat(pd.getStatus()).isEqualTo(401);
        assertThat(pd.getTitle()).isEqualTo("Unauthorized");
    }

    @Test
    @DisplayName("WebExchangeBindException -> 400 with field errors")
    void handleValidation() {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "test");
        bindingResult.addError(new FieldError("test", "originalUrl", "must be a valid URL"));
        bindingResult.addError(new FieldError("test", "title", "size must be between 0 and 100"));

        WebExchangeBindException ex = new WebExchangeBindException(
                null, bindingResult);

        ProblemDetail pd = handler.handleValidation(ex);

        assertThat(pd.getStatus()).isEqualTo(400);
        assertThat(pd.getTitle()).isEqualTo("Validation Failed");
        assertThat(pd.getProperties()).containsKey("errors");
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) pd.getProperties().get("errors");
        assertThat(errors).hasSize(2);
        assertThat(errors.get(0)).contains("originalUrl");
    }

    @Test
    @DisplayName("Generic Exception -> 500")
    void handleGeneric() {
        ProblemDetail pd = handler.handleGeneric(new RuntimeException("Unexpected error"));

        assertThat(pd.getStatus()).isEqualTo(500);
        assertThat(pd.getTitle()).isEqualTo("Internal Server Error");
        assertThat(pd.getDetail()).isEqualTo("An unexpected error occurred");
    }

    @Test
    @DisplayName("falls back to exception message when MessageSource fails")
    void handleWithMessageSourceFallback() {
        when(messageSource.getMessage(eq("error.shortcode.notfound"), any(), any(Locale.class)))
                .thenThrow(new RuntimeException("Message not found"));

        ProblemDetail pd = handler.handleShortCodeNotFound(
                new ShortCodeNotFoundException("abc"), Locale.ENGLISH);

        assertThat(pd.getDetail()).isEqualTo("Short code not found: abc");
    }
}
