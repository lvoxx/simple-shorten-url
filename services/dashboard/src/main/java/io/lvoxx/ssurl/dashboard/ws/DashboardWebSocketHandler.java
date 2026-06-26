package io.lvoxx.ssurl.dashboard.ws;

import java.net.URI;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.lvoxx.ssurl.dashboard.dto.response.DashboardLiveTick;
import io.lvoxx.ssurl.dashboard.security.JwtAccessTokenValidator;
import io.lvoxx.ssurl.dashboard.service.DashboardBroadcaster;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Streams live click ticks to authenticated clients. The browser WebSocket API
 * can't set headers, so the access token arrives as a {@code ?token=} query
 * param and is validated on handshake; invalid/absent tokens are closed.
 *
 * <p>
 * v1 limitation: ticks are not per-connection ownership-filtered — every
 * authenticated client receives the global tick stream and filters client-side
 * to the codes it displays. Acceptable for the dashboard; revisit if tick
 * payloads ever carry sensitive data.
 */
@Slf4j
@Component
public class DashboardWebSocketHandler implements WebSocketHandler {

    private final DashboardBroadcaster broadcaster;
    private final JwtAccessTokenValidator validator;
    private final ObjectMapper objectMapper;

    public DashboardWebSocketHandler(DashboardBroadcaster broadcaster,
            JwtAccessTokenValidator validator, ObjectMapper objectMapper) {
        this.broadcaster = broadcaster;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String token = tokenOf(session.getHandshakeInfo().getUri());
        if (token == null || !validator.isValidAccessToken(token)) {
            return session.close(CloseStatus.POLICY_VIOLATION);
        }

        Flux<WebSocketMessage> outbound = broadcaster.stream()
                .map(tick -> session.textMessage(toJson(tick)));

        // .and(receive) keeps the session alive and completes on client close.
        return session.send(outbound).and(session.receive().then());
    }

    private static String tokenOf(URI uri) {
        Map<String, String> params = UriComponentsBuilder.fromUri(uri).build()
                .getQueryParams().toSingleValueMap();
        return params.get("token");
    }

    private String toJson(DashboardLiveTick tick) {
        try {
            return objectMapper.writeValueAsString(tick);
        } catch (Exception e) {
            log.warn("Failed to serialise live tick: {}", e.getMessage());
            return "{}";
        }
    }
}
