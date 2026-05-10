package io.lvoxx.ssurl.api_service.repository;

import io.lvoxx.ssurl.common.domain.RefreshToken;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface RefreshTokenRepository extends ReactiveCrudRepository<RefreshToken, Long> {

    Mono<RefreshToken> findByToken(String token);

    Mono<Void> deleteByUserId(Long userId);

    Mono<Void> deleteByToken(String token);
}
