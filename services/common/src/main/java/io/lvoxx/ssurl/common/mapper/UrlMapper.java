package io.lvoxx.ssurl.common.mapper;

import io.lvoxx.ssurl.common.dto.request.CreateUrlRequest;
import io.lvoxx.ssurl.common.dto.response.UrlResponse;
import io.lvoxx.ssurl.common.model.Url;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UrlMapper {

    @Mapping(target = "shortUrl", ignore = true)
    UrlResponse toResponse(Url url);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shortCode", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "clickCount", constant = "0L")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Url toDomain(CreateUrlRequest request);
}
