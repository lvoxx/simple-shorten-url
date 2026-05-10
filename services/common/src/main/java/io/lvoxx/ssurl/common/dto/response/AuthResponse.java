package io.lvoxx.ssurl.common.dto.response;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {}
