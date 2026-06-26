-- Minimal schema for dashboard @DataR2dbcTest slices.
-- Mirrors the production tables the dashboard reads/writes: users + urls (for
-- ownership joins), click_events (raw read model), click_daily_rollup (rollup).

CREATE TABLE IF NOT EXISTS users (
  id        BIGSERIAL PRIMARY KEY,
  username  VARCHAR(100) NOT NULL UNIQUE,
  is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS urls (
  id          BIGSERIAL PRIMARY KEY,
  short_code  VARCHAR(64) NOT NULL,
  user_id     BIGINT REFERENCES users (id),
  title       VARCHAR(255),
  is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS click_events (
  id          BIGSERIAL PRIMARY KEY,
  short_code  VARCHAR(64) NOT NULL,
  ip          VARCHAR(45),
  user_agent  VARCHAR(512),
  referer     VARCHAR(2048),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS click_daily_rollup (
  short_code  VARCHAR(64) NOT NULL,
  day         DATE NOT NULL,
  clicks      BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT pk_click_daily_rollup PRIMARY KEY (short_code, day)
);
