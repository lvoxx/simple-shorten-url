package io.lvoxx.ssurl.dashboard.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import io.lvoxx.ssurl.dashboard.ws.DashboardWebSocketHandler;

/**
 * Wires the reactive WebSocket endpoint {@code /ws/dashboard}. The mapping is
 * ordered ahead of the annotated-controller mapping so the path resolves to the
 * WebSocket handler rather than a 404.
 */
@Configuration
public class WebSocketConfig {

    public static final String PATH = "/ws/dashboard";

    @Bean
    public HandlerMapping dashboardWebSocketMapping(DashboardWebSocketHandler handler) {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(Map.of(PATH, (WebSocketHandler) handler));
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
