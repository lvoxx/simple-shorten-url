CREATE SEQUENCE IF NOT EXISTS click_events_id_seq INCREMENT BY 50 START
WITH
    1;

CREATE TABLE
    IF NOT EXISTS click_events (
        id BIGINT NOT NULL DEFAULT nextval ('click_events_id_seq'),
        short_code VARCHAR(64) NOT NULL,
        ip VARCHAR(45),
        user_agent VARCHAR(512),
        referer VARCHAR(2048),
        created_at TIMESTAMPTZ NOT NULL,
        CONSTRAINT pk_click_events PRIMARY KEY (id)
    );

-- Core lookup indexes
CREATE INDEX IF NOT EXISTS idx_click_short_code ON click_events (short_code);

CREATE INDEX IF NOT EXISTS idx_click_created_at ON click_events (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_click_ip ON click_events (ip)
WHERE
    ip IS NOT NULL;

-- Composite index for per-code time-range queries (most common dashboard query)
CREATE INDEX IF NOT EXISTS idx_click_code_created_at ON click_events (short_code, created_at DESC);

-- Partial composite for IP stats per code
CREATE INDEX IF NOT EXISTS idx_click_code_ip ON click_events (short_code, ip)
WHERE
    ip IS NOT NULL;

COMMENT ON TABLE click_events IS 'Persistent store of URL click events consumed from Kafka analytics-events topic';

COMMENT ON COLUMN click_events.short_code IS 'The short code that was resolved';

COMMENT ON COLUMN click_events.ip IS 'Client IP (may be X-Forwarded-For), nullable';

COMMENT ON COLUMN click_events.user_agent IS 'HTTP User-Agent header, nullable';

COMMENT ON COLUMN click_events.referer IS 'HTTP Referer header, nullable';

COMMENT ON COLUMN click_events.created_at IS 'Event creation time (epoch ms from Avro, stored as TIMESTAMPTZ)';

-- ─────────────────────────────────────────────────────────────────────────────
-- Pre-aggregated daily rollup
--
-- Maintained incrementally by the dashboard Kafka consumer (UPSERT per batch):
--   INSERT ... ON CONFLICT (short_code, day) DO UPDATE SET clicks = clicks + EXCLUDED.clicks
--
-- Time-series and top-link dashboard queries read this small table instead of
-- scanning the high-volume click_events table — the core of the performance
-- pipeline (raw events are kept only for drill-down and cold rebuilds).
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE
    IF NOT EXISTS click_daily_rollup (
        short_code VARCHAR(64) NOT NULL,
        day DATE NOT NULL,
        clicks BIGINT NOT NULL DEFAULT 0,
        CONSTRAINT pk_click_daily_rollup PRIMARY KEY (short_code, day)
    );

-- Range scans by day for cross-link aggregations (overview/top-N).
CREATE INDEX IF NOT EXISTS idx_rollup_day ON click_daily_rollup (day DESC);

COMMENT ON TABLE click_daily_rollup IS 'Per-(short_code, day) click counts; incremental rollup powering dashboard time-series and top-N reads';

COMMENT ON COLUMN click_daily_rollup.day IS 'UTC/local date bucket (from click_events.created_at) the count belongs to';

COMMENT ON COLUMN click_daily_rollup.clicks IS 'Total clicks for the short_code on this day';