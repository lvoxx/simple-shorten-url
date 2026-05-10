package io.lvoxx.ssurl.api_service.repository;

import io.lvoxx.ssurl.common.domain.Url;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UrlRepository extends ReactiveCrudRepository<Url, Long> {

    Mono<Url> findByShortCode(String shortCode);

    Mono<Url> findByShortCodeAndIsActive(String shortCode, boolean isActive);

    Flux<Url> findAllByUserId(Long userId);

    Mono<Long> countByUserId(Long userId);

    @Modifying
    @Query("UPDATE urls SET click_count = click_count + 1 WHERE short_code = :shortCode")
    Mono<Void> incrementClickCount(String shortCode);
}
