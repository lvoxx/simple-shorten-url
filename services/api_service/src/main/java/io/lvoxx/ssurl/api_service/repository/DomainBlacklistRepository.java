package io.lvoxx.ssurl.api_service.repository;

import io.lvoxx.ssurl.common.domain.DomainBlacklist;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface DomainBlacklistRepository extends ReactiveCrudRepository<DomainBlacklist, Long> {

    Mono<Boolean> existsByDomain(String domain);

    Mono<DomainBlacklist> findByDomain(String domain);
}
