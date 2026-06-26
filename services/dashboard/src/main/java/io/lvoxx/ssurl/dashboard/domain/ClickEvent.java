package io.lvoxx.ssurl.dashboard.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Read-model row persisted from the {@code analytics-events} Kafka topic.
 *
 * <p>
 * This is the dashboard service's own copy of a click (CQRS read model) — it is
 * intentionally separate from {@code analytics_worker}'s richer {@code analytics}
 * table. Raw rows back drill-down queries; aggregate reads are served from the
 * {@code click_daily_rollup} table and Redis counters instead of scanning here.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("click_events")
public class ClickEvent {

    @Id
    private Long id;

    @Column("short_code")
    private String shortCode;

    private String ip;

    @Column("user_agent")
    private String userAgent;

    private String referer;

    @Column("created_at")
    private LocalDateTime createdAt;
}
