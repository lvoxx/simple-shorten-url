CREATE TABLE IF NOT EXISTS analytics (
  id          BIGSERIAL,
  short_code  VARCHAR(10) NOT NULL,
  ip          VARCHAR(45),
  user_agent  TEXT,
  referer     TEXT,
  country     VARCHAR(10),
  created_at  TIMESTAMP NOT NULL DEFAULT NOW()
) PARTITION BY RANGE (created_at);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'analytics_default') THEN
    CREATE TABLE analytics_default PARTITION OF analytics
      FOR VALUES FROM ('1900-01-01') TO ('2100-01-01');
  END IF;
END $$;
