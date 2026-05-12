package io.lvoxx.ssurl.redirect_service.exception;

import io.lvoxx.ssurl.common.exception.ShortCodeNotFoundException;
import io.lvoxx.ssurl.common.exception.UrlExpiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;

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
    void handleNotFound() {
        when(messageSource.getMessage(eq("error.shortcode.notfound"), any(), any(Locale.class)))
                .thenReturn("Short code 'abc' does not exist");

        ProblemDetail pd = handler.handleNotFound(
                new ShortCodeNotFoundException("abc"), Locale.ENGLISH);

        assertThat(pd.getStatus()).isEqualTo(404);
        assertThat(pd.getDetail()).isEqualTo("Short code 'abc' does not exist");
    }

    @Test
    @DisplayName("UrlExpiredException -> 410")
    void handleExpired() {
        when(messageSource.getMessage(eq("error.shortcode.expired"), any(), any(Locale.class)))
                .thenReturn("Short code 'abc' has expired");

        ProblemDetail pd = handler.handleExpired(
                new UrlExpiredException("abc"), Locale.ENGLISH);

        assertThat(pd.getStatus()).isEqualTo(410);
        assertThat(pd.getDetail()).isEqualTo("Short code 'abc' has expired");
    }

    @Test
    @DisplayName("Generic Exception -> 500")
    void handleGeneral() {
        ProblemDetail pd = handler.handleGeneral(new RuntimeException("Unexpected"));

        assertThat(pd.getStatus()).isEqualTo(500);
        assertThat(pd.getDetail()).isEqualTo("An unexpected error occurred");
    }

    @Test
    @DisplayName("falls back to exception message when MessageSource fails")
    void handlesMessageSourceFallback() {
        when(messageSource.getMessage(eq("error.shortcode.notfound"), any(), any(Locale.class)))
                .thenThrow(new RuntimeException("Message not found"));

        ProblemDetail pd = handler.handleNotFound(
                new ShortCodeNotFoundException("abc"), Locale.ENGLISH);

        assertThat(pd.getDetail()).isEqualTo("Short code not found: abc");
    }
}
