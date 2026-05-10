CREATE TABLE domain_blacklist (
  id          BIGSERIAL PRIMARY KEY,
  domain      VARCHAR(255) UNIQUE NOT NULL,
  reason      VARCHAR(255) DEFAULT 'Security/Abuse prevention',
  created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_domain_blacklist_domain ON domain_blacklist(domain);