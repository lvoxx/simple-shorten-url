package io.lvoxx.ssurl.dashboard.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.lvoxx.ssurl.common.util.Constants;
import io.lvoxx.ssurl.dashboard.dto.response.CodeStats;
import io.lvoxx.ssurl.dashboard.dto.response.DashboardOverview;
import io.lvoxx.ssurl.dashboard.dto.response.TimeSeriesPoint;
import io.lvoxx.ssurl.dashboard.dto.response.TopItem;
import io.lvoxx.ssurl.dashboard.security.CurrentUser;
import io.lvoxx.ssurl.dashboard.service.DashboardService;
import io.lvoxx.ssurl.dashboard.util.Ranges;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

/**
 * Analytics read API for the authenticated user's links. Every endpoint is
 * user-scoped (ownership enforced in the query layer) and served from the
 * rollup table + Redis cache.
 */
@RestController
@RequestMapping(Constants.ApiPaths.DASHBOARD)
@Tag(name = "Dashboard", description = "Per-user link analytics")
@SecurityRequirement(name = Constants.Beans.BEARER_AUTH)
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUser currentUser;

    public DashboardController(DashboardService dashboardService, CurrentUser currentUser) {
        this.dashboardService = dashboardService;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Headline metrics for the current user's links")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Overview metrics",
                    content = @Content(schema = @Schema(implementation = DashboardOverview.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/overview")
    public Mono<DashboardOverview> overview(
            @Parameter(description = "Time range: 24h, 7d, 14d, 30d, 90d", example = "7d")
            @RequestParam(defaultValue = "7d") String range) {
        return currentUser.username()
                .flatMap(user -> dashboardService.overview(user, Ranges.toDays(range)));
    }

    @Operation(summary = "Clicks over time (daily buckets), optionally for one link")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Daily click series"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/timeseries")
    public Mono<List<TimeSeriesPoint>> timeseries(
            @Parameter(description = "Time range", example = "30d")
            @RequestParam(defaultValue = "30d") String range,
            @Parameter(description = "Optional short code to scope the series to a single link")
            @RequestParam(required = false) String code) {
        return currentUser.username()
                .flatMap(user -> dashboardService.timeseries(user, code, Ranges.toDays(range)));
    }

    @Operation(summary = "Top links by clicks")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranked links"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/top/links")
    public Mono<List<TopItem>> topLinks(
            @RequestParam(defaultValue = "30d") String range,
            @RequestParam(defaultValue = "10") int limit) {
        return currentUser.username()
                .flatMap(user -> dashboardService.topLinks(user, Ranges.toDays(range), Ranges.clampLimit(limit)));
    }

    @Operation(summary = "Top referers by clicks")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranked referers"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/top/referers")
    public Mono<List<TopItem>> topReferers(
            @RequestParam(defaultValue = "30d") String range,
            @RequestParam(defaultValue = "10") int limit) {
        return currentUser.username()
                .flatMap(user -> dashboardService.topReferers(user, Ranges.toDays(range), Ranges.clampLimit(limit)));
    }

    @Operation(summary = "Detailed stats for a single link")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Per-link stats",
                    content = @Content(schema = @Schema(implementation = CodeStats.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Link not found or not owned by the user")
    })
    @GetMapping("/links/{shortCode}")
    public Mono<CodeStats> linkStats(
            @Parameter(description = "Short code", example = "1aB3xZ") @PathVariable String shortCode,
            @RequestParam(defaultValue = "30d") String range) {
        return currentUser.username()
                .flatMap(user -> dashboardService.codeStats(user, shortCode, Ranges.toDays(range)));
    }
}
