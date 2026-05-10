package io.lvoxx.ssurl.common.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

public record CreateUrlRequest(
        @NotBlank(message = "{url.originalUrl.notBlank}")
        @URL(message = "{url.originalUrl.invalidUrl}")
        String originalUrl,

        @Size(max = 100, message = "{url.title.size}")
        String title,

        LocalDateTime expireAt
) {}
