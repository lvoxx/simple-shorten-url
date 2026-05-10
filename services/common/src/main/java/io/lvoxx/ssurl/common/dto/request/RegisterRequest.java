package io.lvoxx.ssurl.common.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "{auth.username.notBlank}")
        @Size(min = 3, max = 50, message = "{auth.username.size}")
        String username,

        @NotBlank(message = "{auth.email.notBlank}")
        @Email(message = "{auth.email.invalid}")
        String email,

        @NotBlank(message = "{auth.password.notBlank}")
        @Size(min = 8, max = 100, message = "{auth.password.size}")
        String password
) {}
