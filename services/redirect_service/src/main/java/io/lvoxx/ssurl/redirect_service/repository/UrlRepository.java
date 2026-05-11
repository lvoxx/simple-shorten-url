package io.lvoxx.ssurl.redirect_service.repository;

import io.lvoxx.ssurl.common.model.Url;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UrlRepository extends ReactiveCrudRepository<Url, Long> {

    Mono<Url> findByShortCodeAndIsActive(String shortCode, boolean isActive);

    @Modifying
    @Query("UPDATE urls SET click_count = click_count + 1 WHERE short_code = :shortCode")
    Mono<Void> incrementClickCount(String shortCode);
}
