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

# ────── Service paths ──────
SERVICES_DIR  := services
MW            := $(SERVICES_DIR)/mvnw
API_DIR       := $(SERVICES_DIR)/api_service
REDIRECT_DIR  := $(SERVICES_DIR)/redirect_service
ANALYTICS_DIR := $(SERVICES_DIR)/analytics_worker

# Docker image / container names
API_IMG       := ssurl-api
REDIRECT_IMG  := ssurl-redirect
ANALYTICS_IMG := ssurl-analytics

# Ports
API_PORT       := 8080
REDIRECT_PORT  := 8081
ANALYTICS_PORT := 8082

# Service env file (create docker/.env.services with DB_URL, DB_USERNAME, DB_PASSWORD, REDIS_HOST, JWT_SECRET, etc.)
SVC_ENV        := $(DC_DIR)/.env.services

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
	@printf "\n$(U)Build & Test:$(NC)\n"
	@printf "  $(CY)%-28s$(NC) %s\n" "build"             "Build all service JARs"
	@printf "  $(CY)%-28s$(NC) %s\n" "build-api"         "Build api_service JAR"
	@printf "  $(CY)%-28s$(NC) %s\n" "build-redirect"    "Build redirect_service JAR"
	@printf "  $(CY)%-28s$(NC) %s\n" "build-analytics"   "Build analytics_worker JAR"
	@printf "  $(CY)%-28s$(NC) %s\n" "test"              "Run all service tests"
	@printf "  $(CY)%-28s$(NC) %s\n" "test-api"          "Run api_service tests"
	@printf "  $(CY)%-28s$(NC) %s\n" "test-redirect"     "Run redirect_service tests"
	@printf "  $(CY)%-28s$(NC) %s\n" "test-analytics"    "Run analytics_worker tests"
	@printf "\n$(U)Docker:$(NC)\n"
	@printf "  $(CY)%-28s$(NC) %s\n" "docker-build"           "Build all Docker images"
	@printf "  $(CY)%-28s$(NC) %s\n" "docker-build-api"       "Build ssurl-api Docker image"
	@printf "  $(CY)%-28s$(NC) %s\n" "docker-build-redirect"  "Build ssurl-redirect Docker image"
	@printf "  $(CY)%-28s$(NC) %s\n" "docker-build-analytics" "Build ssurl-analytics Docker image"
	@printf "\n$(U)Service lifecycle:$(NC)\n"
	@printf "  $(CY)%-28s$(NC) %s\n" "run"               "Run all service containers"
	@printf "  $(CY)%-28s$(NC) %s\n" "run-api"           "Run api_service container"
	@printf "  $(CY)%-28s$(NC) %s\n" "run-redirect"      "Run redirect_service container"
	@printf "  $(CY)%-28s$(NC) %s\n" "run-analytics"     "Run analytics_worker container"
	@printf "  $(CY)%-28s$(NC) %s\n" "stop / kill"       "Stop all service containers"
	@printf "  $(CY)%-28s$(NC) %s\n" "stop-api"          "Stop api_service container"
	@printf "  $(CY)%-28s$(NC) %s\n" "stop-redirect"     "Stop redirect_service container"
	@printf "  $(CY)%-28s$(NC) %s\n" "stop-analytics"    "Stop analytics_worker container"
	@printf "  $(CY)%-28s$(NC) %s\n" "restart"           "Restart all service containers"
	@printf "  $(CY)%-28s$(NC) %s\n" "restart-api"       "Restart api_service"
	@printf "  $(CY)%-28s$(NC) %s\n" "restart-redirect"  "Restart redirect_service"
	@printf "  $(CY)%-28s$(NC) %s\n" "restart-analytics" "Restart analytics_worker"
	@printf "  $(CY)%-28s$(NC) %s\n" "reload"            "Full rebuild + restart all"
	@printf "  $(CY)%-28s$(NC) %s\n" "reload-api"        "Rebuild JAR/image + restart api_service"
	@printf "  $(CY)%-28s$(NC) %s\n" "reload-redirect"   "Rebuild JAR/image + restart redirect_service"
	@printf "  $(CY)%-28s$(NC) %s\n" "reload-analytics"  "Rebuild JAR/image + restart analytics_worker"
	@printf "  $(CY)%-28s$(NC) %s\n" "logs"              "Tail logs from all service containers"
	@printf "  $(CY)%-28s$(NC) %s\n" "logs-api"          "Tail api_service logs"
	@printf "  $(CY)%-28s$(NC) %s\n" "logs-redirect"     "Tail redirect_service logs"
	@printf "  $(CY)%-28s$(NC) %s\n" "logs-analytics"    "Tail analytics_worker logs"
	@printf "  $(CY)%-28s$(NC) %s\n" "ps"                "Show running service containers"
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

# ────── Build JARs ──────
build: build-api build-redirect build-analytics ## Build all service JARs

build-api: ## Build api_service JAR
	@printf "$(SEP)" "Building api_service"
	$(MW) clean package -f $(API_DIR)/pom.xml
	@printf "\n$(GN)✔  api_service JAR built$(NC)\n\n"

build-redirect: ## Build redirect_service JAR
	@printf "$(SEP)" "Building redirect_service"
	$(MW) clean package -f $(REDIRECT_DIR)/pom.xml
	@printf "\n$(GN)✔  redirect_service JAR built$(NC)\n\n"

build-analytics: ## Build analytics_worker JAR
	@printf "$(SEP)" "Building analytics_worker"
	$(MW) clean package -f $(ANALYTICS_DIR)/pom.xml
	@printf "\n$(GN)✔  analytics_worker JAR built$(NC)\n\n"

# ────── Tests ──────
test: test-api test-redirect test-analytics ## Run all service tests

test-api: ## Run api_service tests
	@printf "$(SEP)" "Testing api_service"
	$(MW) test -f $(API_DIR)/pom.xml

test-redirect: ## Run redirect_service tests
	@printf "$(SEP)" "Testing redirect_service"
	$(MW) test -f $(REDIRECT_DIR)/pom.xml

test-analytics: ## Run analytics_worker tests
	@printf "$(SEP)" "Testing analytics_worker"
	$(MW) test -f $(ANALYTICS_DIR)/pom.xml

# ────── Docker Images ──────
docker-build: docker-build-api docker-build-redirect docker-build-analytics ## Build all Docker images

docker-build-api: build-api ## Build ssurl-api Docker image
	@printf "$(SEP)" "Building ssurl-api image"
	docker build -f $(API_DIR)/Dockerfile -t $(API_IMG) $(SERVICES_DIR)
	@printf "\n$(GN)✔  ssurl-api image built$(NC)\n\n"

docker-build-redirect: build-redirect ## Build ssurl-redirect Docker image
	@printf "$(SEP)" "Building ssurl-redirect image"
	docker build -f $(REDIRECT_DIR)/Dockerfile -t $(REDIRECT_IMG) $(SERVICES_DIR)
	@printf "\n$(GN)✔  ssurl-redirect image built$(NC)\n\n"

docker-build-analytics: build-analytics ## Build ssurl-analytics Docker image
	@printf "$(SEP)" "Building ssurl-analytics image"
	docker build -f $(ANALYTICS_DIR)/Dockerfile -t $(ANALYTICS_IMG) $(SERVICES_DIR)
	@printf "\n$(GN)✔  ssurl-analytics image built$(NC)\n\n"

# ────── Service Containers: Run ──────
run: run-api run-redirect run-analytics ## Run all service containers

run-api: docker-build-api ## Run api_service container (builds image first)
	@printf "$(SEP)" "Starting api_service"
	@docker rm -f $(API_IMG) 2>/dev/null || true
	docker run -d --name $(API_IMG) --restart unless-stopped \
		-p $(API_PORT):$(API_PORT) \
		$(if $(wildcard $(SVC_ENV)),--env-file $(SVC_ENV),) \
		$(API_IMG)
	@printf "\n$(GN)✔  api_service running → http://localhost:$(API_PORT)$(NC)\n\n"

run-redirect: docker-build-redirect ## Run redirect_service container (builds image first)
	@printf "$(SEP)" "Starting redirect_service"
	@docker rm -f $(REDIRECT_IMG) 2>/dev/null || true
	docker run -d --name $(REDIRECT_IMG) --restart unless-stopped \
		-p $(REDIRECT_PORT):$(REDIRECT_PORT) \
		$(if $(wildcard $(SVC_ENV)),--env-file $(SVC_ENV),) \
		$(REDIRECT_IMG)
	@printf "\n$(GN)✔  redirect_service running → http://localhost:$(REDIRECT_PORT)$(NC)\n\n"

run-analytics: docker-build-analytics ## Run analytics_worker container (builds image first)
	@printf "$(SEP)" "Starting analytics_worker"
	@docker rm -f $(ANALYTICS_IMG) 2>/dev/null || true
	docker run -d --name $(ANALYTICS_IMG) --restart unless-stopped \
		-p $(ANALYTICS_PORT):$(ANALYTICS_PORT) \
		$(if $(wildcard $(SVC_ENV)),--env-file $(SVC_ENV),) \
		$(ANALYTICS_IMG)
	@printf "\n$(GN)✔  analytics_worker running$(NC)\n\n"

# ────── Service Containers: Stop / Kill ──────
stop kill: stop-api stop-redirect stop-analytics ## Stop (and remove) all service containers

stop-api kill-api: ## Stop and remove api_service container
	@printf "$(SEP)" "Stopping api_service"
	@docker rm -f $(API_IMG) 2>/dev/null || true
	@printf "\n$(GN)✔  api_service stopped$(NC)\n\n"

stop-redirect kill-redirect: ## Stop and remove redirect_service container
	@printf "$(SEP)" "Stopping redirect_service"
	@docker rm -f $(REDIRECT_IMG) 2>/dev/null || true
	@printf "\n$(GN)✔  redirect_service stopped$(NC)\n\n"

stop-analytics kill-analytics: ## Stop and remove analytics_worker container
	@printf "$(SEP)" "Stopping analytics_worker"
	@docker rm -f $(ANALYTICS_IMG) 2>/dev/null || true
	@printf "\n$(GN)✔  analytics_worker stopped$(NC)\n\n"

# ────── Service Containers: Restart ──────
restart: restart-api restart-redirect restart-analytics ## Restart all service containers

restart-api: stop-api run-api ## Restart api_service container
restart-redirect: stop-redirect run-redirect ## Restart redirect_service container
restart-analytics: stop-analytics run-analytics ## Restart analytics_worker container

# ────── Service Containers: Reload (full rebuild cycle) ──────
reload: reload-api reload-redirect reload-analytics ## Full rebuild + restart all services

reload-api: build-api stop-api run-api ## Rebuild JAR + image, then restart api_service
reload-redirect: build-redirect stop-redirect run-redirect ## Rebuild JAR + image, then restart redirect_service
reload-analytics: build-analytics stop-analytics run-analytics ## Rebuild JAR + image, then restart analytics_worker

# ────── Service Containers: Logs ──────
logs: ## Tail logs from all service containers
	@docker logs -f $(API_IMG) $(REDIRECT_IMG) $(ANALYTICS_IMG) 2>/dev/null || true

logs-api: ## Tail api_service container logs
	@docker logs -f $(API_IMG)

logs-redirect: ## Tail redirect_service container logs
	@docker logs -f $(REDIRECT_IMG)

logs-analytics: ## Tail analytics_worker container logs
	@docker logs -f $(ANALYTICS_IMG)

# ────── Service Containers: Status ──────
ps: ## Show running service containers
	@printf "$(SEP)" "Service containers"
	@docker ps --filter "name=ssurl-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
