CREATE TABLE urls (
  id            BIGSERIAL PRIMARY KEY,
  short_code    VARCHAR(10) UNIQUE NOT NULL,
  original_url  TEXT NOT NULL,
  user_id       BIGINT REFERENCES users(id) ON DELETE SET NULL,
  title         VARCHAR(255),
  is_active     BOOLEAN NOT NULL DEFAULT TRUE,
  click_count   BIGINT NOT NULL DEFAULT 0,
  expire_at     TIMESTAMP,
  created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by    VARCHAR(255),
  updated_by    VARCHAR(255)
);

CREATE UNIQUE INDEX idx_urls_short_code ON urls(short_code);
CREATE INDEX idx_urls_user_id ON urls(user_id);
CREATE INDEX idx_urls_expire_at ON urls(expire_at) WHERE expire_at IS NOT NULL;
