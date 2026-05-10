package io.lvoxx.ssurl.api_service.service;

import io.lvoxx.ssurl.common.dto.request.LoginRequest;
import io.lvoxx.ssurl.common.dto.request.RegisterRequest;
import io.lvoxx.ssurl.common.dto.response.AuthResponse;
import io.lvoxx.ssurl.common.dto.response.UserResponse;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;

public interface AuthService {

    Mono<UserResponse> register(RegisterRequest request);

    Mono<AuthResponse> login(LoginRequest request, ServerHttpResponse response);

    Mono<AuthResponse> refresh(String refreshToken);

    Mono<Void> logout(String refreshToken, ServerHttpResponse response);
}
