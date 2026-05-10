package io.lvoxx.ssurl.common.dto.request;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateUrlRequest(
        @Size(max = 100, message = "{url.title.size}")
        String title,

        LocalDateTime expireAt,

        Boolean isActive
) {}
