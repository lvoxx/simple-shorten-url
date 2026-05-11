package io.lvoxx.ssurl.api_service.repository;

import io.lvoxx.ssurl.common.model.Url;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UrlRepository extends ReactiveCrudRepository<Url, Long> {

    Mono<Url> findByShortCode(String shortCode);

    Mono<Url> findByShortCodeAndIsActive(String shortCode, boolean isActive);

    @Query("SELECT * FROM urls WHERE user_id = :userId ORDER BY id DESC LIMIT :size")
    Flux<Url> findTopByUserIdOrderByIdDesc(Long userId, int size);

    @Query("SELECT * FROM urls WHERE user_id = :userId AND id < :cursor ORDER BY id DESC LIMIT :size")
    Flux<Url> findByUserIdAndIdLessThanOrderByIdDesc(Long userId, Long cursor, int size);

    Mono<Long> countByUserId(Long userId);

    @Modifying
    @Query("UPDATE urls SET click_count = click_count + 1 WHERE short_code = :shortCode")
    Mono<Void> incrementClickCount(String shortCode);
}
