package io.lvoxx.ssurl.common.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "{auth.username.notBlank}")
        String username,

        @NotBlank(message = "{auth.password.notBlank}")
        String password
) {}
