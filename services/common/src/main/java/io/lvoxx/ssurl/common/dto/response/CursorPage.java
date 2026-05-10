package io.lvoxx.ssurl.common.dto.response;

import java.util.List;

public record CursorPage<T>(
        List<T> content,
        Long nextCursor,
        boolean hasNext
) {}
