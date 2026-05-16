SHELL := /bin/bash
.DEFAULT_GOAL := help

# ────── Paths ──────
DC_DIR       := docker
DC_POSTGRES  := $(DC_DIR)/docker-compose.postgres.yml
DC_REDIS     := $(DC_DIR)/docker-compose.redis.yml
DC_KAFKA     := $(DC_DIR)/docker-compose.kafka.yml
DC_ENV       := --env-file $(DC_DIR)/.env.dev
DC_ALL       := -f $(DC_POSTGRES) -f $(DC_REDIS) -f $(DC_KAFKA)

SCHEMAS      := database/schemas
API_INIT     := services/api_service/src/test/resources/sql/init-schema.sql
REDIR_INIT   := services/redirect_service/src/test/resources/sql/init-schema.sql
ANLT_INIT    := services/analytics_worker/src/test/resources/sql/init-schema.sql

# ────── Pretty print ──────
B   := \033[1m
U   := \033[4m
BL  := \033[1;34m
GN  := \033[1;32m
YE  := \033[1;33m
RD  := \033[1;31m
CY  := \033[1;36m
NC  := \033[0m
SEP := \n$(B)━━━ %s ─────────────────────────────────$(NC)\n

# ────── Help ──────
help:
	@printf "$(SEP)" "Shorten URL Project"
	@printf "$(B)Usage:$(NC) make $(CY)<target>$(NC)\n\n"
	@printf "$(U)Infrastructure:$(NC)\n"
	@printf "  $(CY)%-28s$(NC) %s\n" "infra-up"        "Start all (Postgres + Redis + Kafka)"
	@printf "  $(CY)%-28s$(NC) %s\n" "infra-stop"       "Stop all"
	@printf "  $(CY)%-28s$(NC) %s\n" "infra-restart"    "Restart all"
	@printf "  $(CY)%-28s$(NC) %s\n" "infra-status"     "Show running containers"
	@printf "  $(CY)%-28s$(NC) %s\n" "infra-logs"       "Tail logs"
	@printf "  $(CY)%-28s$(NC) %s\n" "infra-ps"         "Alias for infra-status"
	@printf "\n$(U)Individual services:$(NC)\n"
	@printf "  $(CY)%-28s$(NC) %s\n" "infra-up-pg"      "Start Postgres + pgAdmin"
	@printf "  $(CY)%-28s$(NC) %s\n" "infra-up-redis"   "Start Redis"
	@printf "  $(CY)%-28s$(NC) %s\n" "infra-up-kafka"   "Start Kafka stack"
	@printf "  $(CY)%-28s$(NC) %s\n" "infra-stop-pg"    "Stop Postgres + pgAdmin"
	@printf "  $(CY)%-28s$(NC) %s\n" "infra-stop-redis" "Stop Redis"
	@printf "  $(CY)%-28s$(NC) %s\n" "infra-stop-kafka" "Stop Kafka stack"
	@printf "\n$(U)Schema sync:$(NC)\n"
	@printf "  $(CY)%-28s$(NC) %s\n" "schema-sync"           "Sync ALL test init-scripts from $(SCHEMAS)"
	@printf "  $(CY)%-28s$(NC) %s\n" "schema-sync-api"       "Sync → api_service"
	@printf "  $(CY)%-28s$(NC) %s\n" "schema-sync-redirect"  "Sync → redirect_service"
	@printf "  $(CY)%-28s$(NC) %s\n" "schema-sync-analytics" "Sync → analytics_worker"
	@printf "\n"

# ────── Infrastructure: ALL ──────
infra-up: ## Start all infrastructure (Postgres + Redis + Kafka)
	@printf "$(SEP)" "Starting all infrastructure"
	docker compose $(DC_ALL) $(DC_ENV) up -d
	@printf "\n$(GN)✔  All services up$(NC)\n\n"

infra-stop: ## Stop all infrastructure
	@printf "$(SEP)" "Stopping all infrastructure"
	docker compose $(DC_ALL) $(DC_ENV) down
	@printf "\n$(GN)✔  All services stopped$(NC)\n\n"

infra-restart: infra-stop infra-up ## Restart all infrastructure

infra-status infra-ps: ## Show running container status
	@printf "$(SEP)" "Infrastructure status"
	@docker compose $(DC_ALL) ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"

infra-logs: ## Tail logs from all services
	@docker compose $(DC_ALL) logs -f

# ────── Infrastructure: Postgres ──────
infra-up-pg: ## Start Postgres + pgAdmin
	@printf "$(SEP)" "Starting Postgres"
	docker compose -f $(DC_POSTGRES) $(DC_ENV) up -d
	@printf "\n$(GN)✔  Postgres + pgAdmin started$(NC)\n\n"

infra-stop-pg: ## Stop Postgres + pgAdmin
	@printf "$(SEP)" "Stopping Postgres"
	docker compose -f $(DC_POSTGRES) $(DC_ENV) down
	@printf "\n$(GN)✔  Postgres stopped$(NC)\n\n"

# ────── Infrastructure: Redis ──────
infra-up-redis: ## Start Redis
	@printf "$(SEP)" "Starting Redis"
	docker compose -f $(DC_REDIS) up -d
	@printf "\n$(GN)✔  Redis started$(NC)\n\n"

infra-stop-redis: ## Stop Redis
	@printf "$(SEP)" "Stopping Redis"
	docker compose -f $(DC_REDIS) down
	@printf "\n$(GN)✔  Redis stopped$(NC)\n\n"

# ────── Infrastructure: Kafka ──────
infra-up-kafka: ## Start Kafka stack
	@printf "$(SEP)" "Starting Kafka stack"
	docker compose -f $(DC_KAFKA) up -d
	@printf "\n$(GN)✔  Kafka stack started$(NC)\n\n"

infra-stop-kafka: ## Stop Kafka stack
	@printf "$(SEP)" "Stopping Kafka stack"
	docker compose -f $(DC_KAFKA) down
	@printf "\n$(GN)✔  Kafka stack stopped$(NC)\n\n"

# ────── Schema Sync ──────
# api_service: users + urls + refresh_tokens + domain_blacklist
schema-sync-api:
	@printf "$(SEP)" "Syncing api_service test schema"
	@printf "$(BL)ℹ$(NC)  Source: $(YE)database/schemas/{users,urls,refresh_tokens,domain_blacklist}.sql$(NC)\n"
	@cat $(SCHEMAS)/users.sql $(SCHEMAS)/urls.sql $(SCHEMAS)/refresh_tokens.sql $(SCHEMAS)/domain_blacklist.sql \
		| sed -E \
			-e '/^\s*$$/d' \
			-e '/^--/d' \
			-e '/PARTITION OF/!s/^CREATE TABLE /CREATE TABLE IF NOT EXISTS /' \
			-e 's/^CREATE UNIQUE INDEX /CREATE UNIQUE INDEX IF NOT EXISTS /' \
			-e 's/^CREATE INDEX /CREATE INDEX IF NOT EXISTS /' \
		> $(API_INIT)
	@printf "$(GN)✔$(NC)  Written → $(CY)$(API_INIT)$(NC)  "
	@printf "($(YE)$$(wc -l < $(API_INIT)) lines$(NC))\n\n"

# redirect_service: users + urls
schema-sync-redirect:
	@printf "$(SEP)" "Syncing redirect_service test schema"
	@printf "$(BL)ℹ$(NC)  Source: $(YE)database/schemas/{users,urls}.sql$(NC)\n"
	@cat $(SCHEMAS)/users.sql $(SCHEMAS)/urls.sql \
		| sed -E \
			-e '/^\s*$$/d' \
			-e '/^--/d' \
			-e 's/^CREATE TABLE /CREATE TABLE IF NOT EXISTS /' \
			-e 's/^CREATE UNIQUE INDEX /CREATE UNIQUE INDEX IF NOT EXISTS /' \
			-e 's/^CREATE INDEX /CREATE INDEX IF NOT EXISTS /' \
		> $(REDIR_INIT)
	@printf "$(GN)✔$(NC)  Written → $(CY)$(REDIR_INIT)$(NC)  "
	@printf "($(YE)$$(wc -l < $(REDIR_INIT)) lines$(NC))\n\n"

# analytics_worker: analytics (skip partition tables, add safe DO block)
schema-sync-analytics:
	@printf "$(SEP)" "Syncing analytics_worker test schema"
	@printf "$(BL)ℹ$(NC)  Source: $(YE)database/schemas/analytics.sql$(NC)\n"
	@cat $(SCHEMAS)/analytics.sql \
		| sed -E \
			-e '/^\s*$$/d' \
			-e '/^--/d' \
			-e '/CREATE TABLE.*PARTITION OF/d' \
			-e '/PARTITION OF/!s/^CREATE TABLE /CREATE TABLE IF NOT EXISTS /' \
			-e 's/^CREATE INDEX /CREATE INDEX IF NOT EXISTS /' \
		> $(ANLT_INIT)
	@printf "DO \$$\$$\nBEGIN\n  IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'analytics_default') THEN\n    CREATE TABLE analytics_default PARTITION OF analytics\n      FOR VALUES FROM ('1900-01-01') TO ('2100-01-01');\n  END IF;\nEND \$$\$$;\n" >> $(ANLT_INIT)
	@printf "$(GN)✔$(NC)  Written → $(CY)$(ANLT_INIT)$(NC)  "
	@printf "($(YE)$$(wc -l < $(ANLT_INIT)) lines$(NC))\n\n"

schema-sync: schema-sync-api schema-sync-redirect schema-sync-analytics ## Sync ALL test init-scripts
	@printf "\n$(GN)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)\n"
	@printf "$(GN)✔  All schemas synced successfully$(NC)\n"
	@printf "$(GN)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)\n\n"
