# Architecture Changelog

## 2026-05-29 16:31 Asia/Saigon

### Added

- Thêm production Docker Compose stack gồm Nginx, 2 Spring Boot app instances, PostgreSQL primary/2 replicas, Redis, Prometheus, Grafana, Loki, Promtail và exporters.
- Thêm PostgreSQL streaming replication với physical slots `replica_1`, `replica_2`.
- Thêm Nginx reverse proxy/load balancing `least_conn`, JSON access log, rate limit và security headers.
- Thêm monitoring stack: Prometheus scrape jobs, Grafana datasource/dashboard provisioning, Loki local storage.
- Thêm structured logging JSON cho Spring Boot: `application.log`, `error.log`, `audit.log`.
- Thêm Promtail scrape jobs cho Spring app logs và Nginx logs.
- Thêm Grafana alert `ERROR spike detected` dựa trên Loki query `{job="spring-error"}`.
- Thêm PostgreSQL slow query logging với ngưỡng 1000ms.
- Thêm Micrometer tracing bridge Brave và Zipkin reporter dependency, nhưng tắt Zipkin export mặc định bằng `ZIPKIN_TRACING_EXPORT_ENABLED=false`.
- Thêm bộ tài liệu `docs/` cho kiến trúc, services, logging, database, operations và ADR.

### Fixed

- Sửa script replica để chạy an toàn trên Windows checkout bằng cách strip CRLF trước khi execute.
- Sửa replica healthcheck dùng database `postgres`, tránh log spam `database "replicator" does not exist`.
- Sửa Loki WAL path sang `/loki/wal` để tránh permission denied với image `grafana/loki:2.9.0`.

### Verified

- `docker compose config --quiet` pass.
- `mvnw -q -DskipTests validate` pass.
- `app-1`, `app-2`, PostgreSQL replicas, Loki, Promtail, Grafana healthy.
- PostgreSQL replication có 2 sender `streaming`, lag `0`.
- Loki query `{job="spring-app"}` trả log thành công.
