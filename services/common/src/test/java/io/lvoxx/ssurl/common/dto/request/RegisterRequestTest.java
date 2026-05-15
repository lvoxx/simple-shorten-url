package io.lvoxx.ssurl.common.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestTest {

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
        var request = new RegisterRequest("user123", "user@example.com", "password123");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("null username should produce violation")
    void nullUsername() {
        var request = new RegisterRequest(null, "user@example.com", "password123");
        var violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    @DisplayName("blank username should produce violation")
    void blankUsername() {
        var request = new RegisterRequest("", "user@example.com", "password123");
        var violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    @DisplayName("username below min length should produce violation")
    void usernameTooShort() {
        var request = new RegisterRequest("ab", "user@example.com", "password123");
        var violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    @DisplayName("username at min length should be valid")
    void usernameAtMinLength() {
        var request = new RegisterRequest("abc", "user@example.com", "password123");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("username exceeding max length should produce violation")
    void usernameTooLong() {
        var request = new RegisterRequest("a".repeat(51), "user@example.com", "password123");
        var violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    @DisplayName("username at max length should be valid")
    void usernameAtMaxLength() {
        var request = new RegisterRequest("a".repeat(50), "user@example.com", "password123");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("null email should produce violation")
    void nullEmail() {
        var request = new RegisterRequest("user123", null, "password123");
        var violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("blank email should produce violation")
    void blankEmail() {
        var request = new RegisterRequest("user123", "", "password123");
        var violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("invalid email format should produce violation")
    void invalidEmail() {
        var request = new RegisterRequest("user123", "not-an-email", "password123");
        var violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("null password should produce violation")
    void nullPassword() {
        var request = new RegisterRequest("user123", "user@example.com", null);
        var violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("blank password should produce violation")
    void blankPassword() {
        var request = new RegisterRequest("user123", "user@example.com", "");
        var violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("password below min length should produce violation")
    void passwordTooShort() {
        var request = new RegisterRequest("user123", "user@example.com", "1234567");
        var violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("password at min length should be valid")
    void passwordAtMinLength() {
        var request = new RegisterRequest("user123", "user@example.com", "12345678");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("password exceeding max length should produce violation")
    void passwordTooLong() {
        var request = new RegisterRequest("user123", "user@example.com", "a".repeat(101));
        var violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("password at max length should be valid")
    void passwordAtMaxLength() {
        var request = new RegisterRequest("user123", "user@example.com", "a".repeat(100));
        assertThat(validator.validate(request)).isEmpty();
    }
}
