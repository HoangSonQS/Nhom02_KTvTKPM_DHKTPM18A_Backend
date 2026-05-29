# SEBook Backend - Architecture Overview

## Quick start

```bash
cp .env.example .env
# Điền các biến thật vào .env
make up
# Truy cập API qua http://localhost
```

## Tài liệu

| File | Nội dung |
|------|----------|
| [architecture.md](./architecture.md) | Kiến trúc tổng quan, sơ đồ, luồng request |
| [services.md](./services.md) | Chi tiết từng container/service |
| [logging.md](./logging.md) | Hệ thống logging, metrics, alert |
| [database.md](./database.md) | PostgreSQL streaming replication |
| [operations.md](./operations.md) | Vận hành, deploy, troubleshoot |
| [cicd.md](./cicd.md) | CI/CD GitHub Actions, DockerHub, EC2 |
| [adr/](./adr/) | Architecture Decision Records |

## Stack

| Thành phần | Công nghệ / version | Vai trò |
|------------|----------------------|---------|
| API gateway | `nginx:alpine` | Reverse proxy, load balancing, rate limit |
| Backend | Spring Boot 3.5.6, Java 17 | SEBook modular monolith API |
| Runtime image | `eclipse-temurin:17-jre-jammy` | Chạy WAR production |
| Database | `pgvector/pgvector:pg15` | PostgreSQL primary + read replicas, pgvector |
| Cache | `redis:7-alpine` | Cache với AOF persistence |
| Metrics | `prom/prometheus:v2.54.1` | Scrape metrics |
| Dashboard | `grafana/grafana:11.2.0` | Monitoring UI, alerting |
| Logs | `grafana/loki:2.9.0`, `grafana/promtail:2.9.0` | Log aggregation |
| Exporters | Postgres, Nginx, Redis exporters | Metrics cho infrastructure |

## Ports

| Service | Host port | Container port | Ghi chú |
|---------|-----------|----------------|---------|
| Nginx | 80 | 80 | Entry point HTTP |
| Nginx | 443 | 443 | Reserved cho HTTPS; chưa có TLS config trong repo |
| App-1 | 8080 | 8080 | Bypass Nginx để test/debug |
| App-2 | 8081 | 8080 | Bypass Nginx để test/debug |
| Grafana | 3000 | 3000 | Monitoring UI |
| Prometheus | 9090 | 9090 | Metrics API/UI |
| Loki | 3100 | 3100 | Log query API |
| PostgreSQL | internal only | 5432 | Chỉ trong Docker network |
| Redis | internal only | 6379 | Chỉ trong Docker network |

## Luồng vận hành nhanh

```bash
make ps
make health
make check-replication
docker compose logs nginx -f --tail=100
```

## Lưu ý bảo mật

`.env.example` chỉ là template. Trước khi deploy thật, đổi tất cả password/key trong `.env`, không commit `.env`, và chỉ public port `80/443`; các port `3000`, `9090`, `3100`, `8080`, `8081` nên giới hạn trong VPN/internal network.
