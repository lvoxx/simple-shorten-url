package io.lvoxx.ssurl.dashboard.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import io.lvoxx.ssurl.dashboard.dto.response.DashboardOverview;
import io.lvoxx.ssurl.dashboard.dto.response.TopItem;
import io.lvoxx.ssurl.dashboard.security.CurrentUser;
import io.lvoxx.ssurl.dashboard.service.DashboardService;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardController Tests")
@Tag("Unit")
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;
    @Mock
    private CurrentUser currentUser;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToController(new DashboardController(dashboardService, currentUser)).build();
        when(currentUser.username()).thenReturn(Mono.just("alice"));
    }

    @Test
    void overview_returnsMetrics_andResolvesRange() {
        when(dashboardService.overview("alice", 7))
                .thenReturn(Mono.just(new DashboardOverview(100, 5, 20, 42, 12.5)));

        client.get().uri("/api/v1/dashboard/overview?range=7d")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalClicks").isEqualTo(100)
                .jsonPath("$.uniqueVisitors").isEqualTo(42)
                .jsonPath("$.trendPct").isEqualTo(12.5);

        verify(dashboardService).overview("alice", 7);
    }

    @Test
    void topLinks_clampsLimit() {
        when(dashboardService.topLinks(eq("alice"), eq(30), eq(50)))
                .thenReturn(Mono.just(List.of(new TopItem("a1", "Hello", 9))));

        client.get().uri("/api/v1/dashboard/top/links?range=30d&limit=999")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].key").isEqualTo("a1");

        verify(dashboardService).topLinks("alice", 30, 50);
    }

    @Test
    void overview_defaultsRangeWhenAbsent() {
        when(dashboardService.overview("alice", 7))
                .thenReturn(Mono.just(new DashboardOverview(0, 0, 0, 0, 0)));

        client.get().uri("/api/v1/dashboard/overview")
                .exchange()
                .expectStatus().isOk();

        verify(dashboardService).overview("alice", 7);
    }
}
