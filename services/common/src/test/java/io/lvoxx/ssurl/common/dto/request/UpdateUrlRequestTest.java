package io.lvoxx.ssurl.common.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateUrlRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("valid request with all fields should have no violations")
    void validRequest() {
        var request = new UpdateUrlRequest("New title", LocalDateTime.now().plusDays(1), true);
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("null title should be valid (optional field)")
    void nullTitle() {
        var request = new UpdateUrlRequest(null, LocalDateTime.now().plusDays(1), true);
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("title exceeding max length should produce violation")
    void titleTooLong() {
        var request = new UpdateUrlRequest("a".repeat(101), LocalDateTime.now().plusDays(1), true);
        var violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("title"));
    }

    @Test
    @DisplayName("title at max length should be valid")
    void titleAtMaxLength() {
        var request = new UpdateUrlRequest("a".repeat(100), LocalDateTime.now().plusDays(1), true);
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("null expireAt should be valid (optional field)")
    void nullExpireAt() {
        var request = new UpdateUrlRequest("title", null, true);
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("null isActive should be valid (optional field)")
    void nullIsActive() {
        var request = new UpdateUrlRequest("title", LocalDateTime.now().plusDays(1), null);
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("all fields null should have no violations")
    void allFieldsNull() {
        var request = new UpdateUrlRequest(null, null, null);
        assertThat(validator.validate(request)).isEmpty();
    }
}
