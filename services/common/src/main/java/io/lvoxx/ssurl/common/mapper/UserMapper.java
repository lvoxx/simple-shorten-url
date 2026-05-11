package io.lvoxx.ssurl.common.mapper;

import io.lvoxx.ssurl.common.dto.response.UserResponse;
import io.lvoxx.ssurl.common.model.User;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "isActive", source = "isActive")
    UserResponse toResponse(User user);
}
