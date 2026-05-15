package io.lvoxx.ssurl.common.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CreateUrlRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("valid request should have no violations")
    void validRequest() {
        var request = new CreateUrlRequest("https://example.com", "My title", LocalDateTime.now().plusDays(1));
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("null originalUrl should produce violation")
    void nullOriginalUrl() {
        var request = new CreateUrlRequest(null, "title", null);
        var violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("originalUrl"));
    }

    @Test
    @DisplayName("blank originalUrl should produce violation")
    void blankOriginalUrl() {
        var request = new CreateUrlRequest("", "title", null);
        var violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("originalUrl"));
    }

    @Test
    @DisplayName("invalid URL format should produce violation")
    void invalidUrl() {
        var request = new CreateUrlRequest("not-a-url", "title", null);
        var violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("originalUrl"));
    }

    @Test
    @DisplayName("null title should be valid (optional field)")
    void nullTitle() {
        var request = new CreateUrlRequest("https://example.com", null, null);
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("title exceeding max length should produce violation")
    void titleTooLong() {
        var request = new CreateUrlRequest("https://example.com", "a".repeat(101), null);
        var violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("title"));
    }

    @Test
    @DisplayName("title at max length should be valid")
    void titleAtMaxLength() {
        var request = new CreateUrlRequest("https://example.com", "a".repeat(100), null);
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("null expireAt should be valid (optional field)")
    void nullExpireAt() {
        var request = new CreateUrlRequest("https://example.com", "title", null);
        assertThat(validator.validate(request)).isEmpty();
    }
}
