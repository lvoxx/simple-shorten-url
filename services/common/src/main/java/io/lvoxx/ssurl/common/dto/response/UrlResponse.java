package io.lvoxx.ssurl.common.dto.response;

import java.time.LocalDateTime;

public record UrlResponse(
        Long id,
        String shortCode,
        String shortUrl,
        String originalUrl,
        String title,
        boolean isActive,
        long clickCount,
        LocalDateTime expireAt,
        LocalDateTime createdAt
) {}
