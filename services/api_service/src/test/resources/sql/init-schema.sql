CREATE TABLE IF NOT EXISTS users (
  id            BIGSERIAL PRIMARY KEY,
  email         VARCHAR(255) UNIQUE NOT NULL,
  username      VARCHAR(100) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role          VARCHAR(20) NOT NULL DEFAULT 'USER',
  is_active     BOOLEAN NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS urls (
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

CREATE TABLE IF NOT EXISTS refresh_tokens (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token       VARCHAR(512) UNIQUE NOT NULL,
  expires_at  TIMESTAMP NOT NULL,
  is_revoked  BOOLEAN NOT NULL DEFAULT FALSE,
  created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS domain_blacklist (
  id          BIGSERIAL PRIMARY KEY,
  domain      VARCHAR(255) UNIQUE NOT NULL,
  reason      VARCHAR(255) DEFAULT 'Security/Abuse prevention',
  created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
