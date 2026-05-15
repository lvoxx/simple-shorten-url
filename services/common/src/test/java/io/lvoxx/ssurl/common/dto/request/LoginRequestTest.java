package io.lvoxx.ssurl.common.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestTest {

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
        var request = new LoginRequest("user1", "password123");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("null username should produce violation")
    void nullUsername() {
        var request = new LoginRequest(null, "password123");
        var violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    @DisplayName("blank username should produce violation")
    void blankUsername() {
        var request = new LoginRequest("", "password123");
        var violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    @DisplayName("null password should produce violation")
    void nullPassword() {
        var request = new LoginRequest("user1", null);
        var violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("blank password should produce violation")
    void blankPassword() {
        var request = new LoginRequest("user1", "");
        var violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("both fields blank should produce two violations")
    void bothBlank() {
        var request = new LoginRequest("", "");
        assertThat(validator.validate(request)).hasSize(2);
    }
}
