COMPOSE := docker compose

.PHONY: help up db down logs ps build reset mysql

help: ## Show available targets
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*## "} {printf "  %-8s %s\n", $$1, $$2}'

up: ## Start MySQL and the app (builds the app image if needed)
	$(COMPOSE) up -d --build --wait

db: ## Start only MySQL (for running the app from your IDE or ./mvnw)
	$(COMPOSE) up -d --wait mysql

down: ## Stop and remove containers (data is kept)
	$(COMPOSE) down

logs: ## Follow logs from all services
	$(COMPOSE) logs -f

ps: ## Show service status and health
	$(COMPOSE) ps

build: ## Rebuild the app image
	$(COMPOSE) build app

reset: ## Stop everything and DELETE the database volume (asks first)
	@printf "This deletes all MySQL data for flagman. Continue? [y/N] "; \
	read ans; \
	if [ "$$ans" = "y" ] || [ "$$ans" = "Y" ]; then \
		$(COMPOSE) down -v; \
	else \
		echo "Aborted."; \
	fi

mysql: ## Open a MySQL shell as the app user
	$(COMPOSE) exec mysql sh -c 'mysql -u"$$MYSQL_USER" -p"$$MYSQL_PASSWORD" "$$MYSQL_DATABASE"'
