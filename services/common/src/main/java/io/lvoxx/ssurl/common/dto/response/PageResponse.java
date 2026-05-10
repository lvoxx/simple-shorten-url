package io.lvoxx.ssurl.common.dto.response;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        long total,
        int page,
        int size
) {}
