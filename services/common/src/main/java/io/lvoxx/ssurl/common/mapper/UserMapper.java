package io.lvoxx.ssurl.common.mapper;

import io.lvoxx.ssurl.common.domain.User;
import io.lvoxx.ssurl.common.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "isActive", source = "active")
    UserResponse toResponse(User user);
}
