package io.lvoxx.ssurl.common.dto.response;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        String role,
        boolean isActive,
        LocalDateTime createdAt
) {}
