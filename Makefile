COMPOSE ?= docker compose -f docker-compose.yml

.PHONY: up down logs scale-app ps health init-db clean config check-replication

up:
	$(COMPOSE) up -d --build

down:
	$(COMPOSE) down

logs:
	$(COMPOSE) logs -f

scale-app:
	@echo "This stack uses fixed app-1/app-2 services because nginx upstreams are explicit."
	@echo "To add more instances, duplicate an app service and add it to nginx/conf.d/default.conf."
	$(COMPOSE) up -d --build app-1 app-2

ps:
	$(COMPOSE) ps

health:
	curl -fsS http://localhost/nginx-health
	curl -fsS http://localhost/actuator/health
	curl -fsS http://localhost:9090/-/healthy
	curl -fsS http://localhost:3000/api/health

init-db:
	./mvnw -DskipTests flyway:migrate

clean:
	$(COMPOSE) down -v --remove-orphans

config:
	$(COMPOSE) config

check-replication:
	sh scripts/check-replication.sh
