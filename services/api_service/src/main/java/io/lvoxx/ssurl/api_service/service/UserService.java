package io.lvoxx.ssurl.api_service.service;

import io.lvoxx.ssurl.common.dto.response.UserResponse;
import reactor.core.publisher.Mono;

public interface UserService {

    Mono<UserResponse> getById(Long id);

    Mono<UserResponse> getByUsername(String username);

    Mono<UserResponse> updateEmail(Long id, String newEmail);

    Mono<Void> deactivate(Long id);
}
