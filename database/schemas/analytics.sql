CREATE TABLE analytics (
  id          BIGSERIAL,
  short_code  VARCHAR(10) NOT NULL,
  ip          INET,
  user_agent  TEXT,
  referer     TEXT,
  country     VARCHAR(10),  -- from IP geo-lookup (optional)
  created_at  TIMESTAMP NOT NULL DEFAULT NOW()
) PARTITION BY RANGE (created_at);

-- Monthly partitions (example)
CREATE TABLE analytics_2024_01 PARTITION OF analytics
  FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE INDEX idx_analytics_short_code ON analytics(short_code);
CREATE INDEX idx_analytics_created_at ON analytics(created_at);