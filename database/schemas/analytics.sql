CREATE TABLE
  ip2location (
    id BIGSERIAL PRIMARY KEY,
    ip_range CIDR NOT NULL, -- hoặc dùng start_ip / end_ip (BIGINT)
    country_code VARCHAR(2),
    country_name VARCHAR(100),
    region VARCHAR(100),
    city VARCHAR(100),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    isp VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW ()
  )
PARTITION BY
  RANGE (created_at);

-- Monthly partitions (example)
CREATE TABLE
  analytics_2024_01 PARTITION OF analytics FOR
VALUES
FROM
  ('2024-01-01') TO ('2024-02-01');

CREATE INDEX idx_analytics_short_code ON analytics (short_code);

CREATE INDEX idx_analytics_created_at ON analytics (created_at);

CREATE INDEX idx_ip2location_range ON ip2location USING GIST (ip_range inet_ops);