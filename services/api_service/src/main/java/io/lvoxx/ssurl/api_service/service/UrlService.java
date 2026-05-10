package io.lvoxx.ssurl.api_service.service;

import io.lvoxx.ssurl.common.dto.request.CreateUrlRequest;
import io.lvoxx.ssurl.common.dto.request.UpdateUrlRequest;
import io.lvoxx.ssurl.common.dto.response.UrlResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UrlService {

    Mono<UrlResponse> createUrl(CreateUrlRequest request, Long userId, String createdBy);

    Mono<UrlResponse> getByShortCode(String shortCode);

    Flux<UrlResponse> listByUser(Long userId);

    Mono<UrlResponse> update(Long id, UpdateUrlRequest request, Long userId);

    Mono<Void> delete(Long id, Long userId);
}
