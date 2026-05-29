# Chi tiết các services

Tài liệu này map trực tiếp từ `docker-compose.yml` và các file config trong repo. Mỗi service có vai trò, cấu hình quan trọng, cách kiểm tra health, xem logs và restart/reload.

## nginx

**Image:** `nginx:alpine`  
**Role:** Entry point HTTP, reverse proxy, load balancer, rate limiting.  
**Port:** `80:80`, `443:443`

### Cấu hình quan trọng

```nginx
worker_processes auto;
worker_connections 1024;
limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;

upstream springboot_backend {
    least_conn;
    server app-1:8080 max_fails=3 fail_timeout=30s;
    server app-2:8080 max_fails=3 fail_timeout=30s;
    keepalive 32;
}
```

`least_conn` ưu tiên instance ít connection hơn. `limit_req_zone` giới hạn request theo IP, giúp chặn burst traffic sớm tại gateway.

### Cách hoạt động

Nginx chỉ forward `/api/` và `/actuator/health` sang Spring Boot. Endpoint `/nginx-health` trả về từ Nginx để healthcheck gateway. Endpoint `/nginx_status` phục vụ `nginx-exporter` và chỉ allow localhost + Docker subnet `172.16.0.0/12`.

### Health check

```bash
curl -fsS http://localhost/nginx-health
```

### Logs

```bash
docker compose logs nginx -f --tail=100
docker compose exec nginx tail -100 /var/log/nginx/access.log
```

### Restart / reload

```bash
docker compose exec nginx nginx -s reload
docker compose restart nginx
```

## app-1

**Image:** `${DOCKERHUB_USERNAME}/sebook-backend:${APP_VERSION:-latest}`  
**Role:** Spring Boot backend instance thứ nhất.  
**Port:** `8080:8080`, internal `8080`

### Cấu hình quan trọng

```yaml
environment:
  SERVER_PORT: 8080
  SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-prod}
  DB_PRIMARY_HOST: postgres-primary
  DB_READ_HOST_1: postgres-replica-1
  DB_READ_HOST_2: postgres-replica-2
  DB_URL: jdbc:postgresql://postgres-primary:5432/${DB_NAME:-sebook}
  REDIS_HOST: redis
  LOGGING_FILE_NAME: /logs/app/application.log
image: ${DOCKERHUB_USERNAME}/sebook-backend:${APP_VERSION:-latest}
pull_policy: always
volumes:
  - ./logs/app:/logs/app
  - ./config/keys:/app/config/keys:ro
```

### Cách hoạt động

App chạy bằng Java 17 JRE, package WAR từ Maven. Container chạy user `spring`, tạo `/logs/app` và đọc JWT key từ `./config/keys`.

### Health check

```bash
curl -fsS http://localhost:8080/actuator/health
```

### Logs

```bash
docker compose logs app-1 -f --tail=100
docker compose exec app-1 ls -la /logs/app/
```

### Restart / reload

```bash
docker compose up -d --no-deps app-1
docker compose restart app-1
```

## app-2

**Image:** `${DOCKERHUB_USERNAME}/sebook-backend:${APP_VERSION:-latest}`  
**Role:** Spring Boot backend instance thứ hai.  
**Port:** `8081:8080`, internal `8080`

### Cấu hình quan trọng

```yaml
environment:
  SERVER_PORT: 8080
  DB_PRIMARY_HOST: postgres-primary
  DB_READ_HOST_1: postgres-replica-1
  DB_READ_HOST_2: postgres-replica-2
image: ${DOCKERHUB_USERNAME}/sebook-backend:${APP_VERSION:-latest}
pull_policy: always
ports:
  - "8081:8080"
```

### Cách hoạt động

Giống `app-1`, nhưng host port là `8081`. Nginx không dùng host port, mà gọi trực tiếp `app-2:8080` qua Docker backend network.

### Health check

```bash
curl -fsS http://localhost:8081/actuator/health
```

### Logs

```bash
docker compose logs app-2 -f --tail=100
```

### Restart / reload

```bash
docker compose up -d --no-deps app-2
docker compose restart app-2
```

## postgres-primary

**Image:** `${POSTGRES_IMAGE:-pgvector/pgvector:pg15}`  
**Role:** PostgreSQL primary cho write traffic và WAL source cho replicas.  
**Port:** internal `5432`

### Cấu hình quan trọng

```conf
max_connections = 200
wal_level = replica
max_wal_senders = 3
max_replication_slots = 3
wal_keep_size = 64MB
hot_standby = on
log_min_duration_statement = 1000
```

### Cách hoạt động

Primary mount `postgres/primary/postgresql.conf`, `pg_hba.conf`, và init script tạo replication user + slots `replica_1`, `replica_2`. `pg_hba.conf` cho phép app và replication từ Docker subnet `172.16.0.0/12` bằng `scram-sha-256`.

### Health check

```bash
docker compose exec postgres-primary pg_isready -U ${DB_USER:-appuser} -d ${DB_NAME:-sebook}
```

### Logs

```bash
docker compose logs postgres-primary -f --tail=100
```

### Restart / reload

```bash
docker compose restart postgres-primary
```

## postgres-replica-1

**Image:** `${POSTGRES_IMAGE:-pgvector/pgvector:pg15}`  
**Role:** Read replica đầu tiên.  
**Port:** internal `5432`

### Cấu hình quan trọng

```yaml
environment:
  PGHOST: postgres-primary
  PGPORT: 5432
  REPLICA_SLOT: replica_1
command:
  - /bin/bash
  - -c
  - tr -d '\r' < /usr/local/bin/init-replica.sh > /tmp/init-replica.sh && /bin/bash /tmp/init-replica.sh
```

### Cách hoạt động

Khi data directory chưa có `PG_VERSION`, script chạy `pg_basebackup -R --slot=replica_1`, ghi `primary_conninfo`, tạo `standby.signal`, sau đó start PostgreSQL standby.

### Health check

```bash
docker compose exec postgres-replica-1 pg_isready -U ${POSTGRES_REPLICATION_USER:-replicator} -d postgres
```

### Logs

```bash
docker compose logs postgres-replica-1 -f --tail=100
```

### Restart / reload

```bash
docker compose restart postgres-replica-1
```

## postgres-replica-2

**Image:** `${POSTGRES_IMAGE:-pgvector/pgvector:pg15}`  
**Role:** Read replica thứ hai.  
**Port:** internal `5432`

### Cấu hình quan trọng

```yaml
environment:
  PGHOST: postgres-primary
  PGPORT: 5432
  REPLICA_SLOT: replica_2
```

### Cách hoạt động

Giống `postgres-replica-1`, nhưng dùng physical slot `replica_2`.

### Health check

```bash
docker compose exec postgres-replica-2 pg_isready -U ${POSTGRES_REPLICATION_USER:-replicator} -d postgres
```

### Logs

```bash
docker compose logs postgres-replica-2 -f --tail=100
```

### Restart / reload

```bash
docker compose restart postgres-replica-2
```

## redis

**Image:** `redis:7-alpine`  
**Role:** Cache.  
**Port:** internal `6379`

### Cấu hình quan trọng

```conf
appendonly yes
appendfsync everysec
maxmemory 256mb
maxmemory-policy allkeys-lru
```

### Cách hoạt động

Redis lưu cache với giới hạn 256MB. Khi đầy memory, policy `allkeys-lru` loại bỏ key ít dùng gần đây nhất. AOF `everysec` cân bằng giữa durability và performance.

### Health check

```bash
docker compose exec redis redis-cli ping
```

### Logs

```bash
docker compose logs redis -f --tail=100
```

### Restart / reload

```bash
docker compose restart redis
```

## prometheus

**Image:** `prom/prometheus:v2.54.1`  
**Role:** Scrape và lưu metrics.  
**Port:** `9090:9090`

### Cấu hình quan trọng

```yaml
global:
  scrape_interval: 15s
scrape_configs:
  - job_name: spring-boot
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: [app-1:8080, app-2:8080]
```

### Cách hoạt động

Prometheus đọc `monitoring/prometheus.yml`, lưu TSDB trong volume `prometheus-data`, retention `15d`.

### Health check

```bash
curl -fsS http://localhost:9090/-/healthy
```

### Logs

```bash
docker compose logs prometheus -f --tail=100
```

### Restart / reload

```bash
curl -X POST http://localhost:9090/-/reload
docker compose restart prometheus
```

## grafana

**Image:** `grafana/grafana:11.2.0`  
**Role:** Dashboard, Explore logs/metrics, alerting.  
**Port:** `3000:3000`

### Cấu hình quan trọng

```yaml
GF_SECURITY_ADMIN_USER: ${GRAFANA_USER:-admin}
GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD:-admin}
volumes:
  - ./monitoring/grafana/provisioning:/etc/grafana/provisioning:ro
  - ./monitoring/grafana/dashboards:/var/lib/grafana/dashboards:ro
```

### Cách hoạt động

Grafana provision datasource Prometheus uid `prometheus`, Loki uid `loki`, dashboard từ `/var/lib/grafana/dashboards`, và alert rule từ `monitoring/grafana/provisioning/alerting/rules.yml`.

### Health check

```bash
curl -fsS http://localhost:3000/api/health
```

### Logs

```bash
docker compose logs grafana -f --tail=100
```

### Restart / reload

```bash
docker compose restart grafana
```

## loki

**Image:** `grafana/loki:2.9.0`  
**Role:** Lưu và query logs.  
**Port:** `3100:3100`

### Cấu hình quan trọng

```yaml
limits_config:
  retention_period: 168h
  ingestion_rate_mb: 16
  ingestion_burst_size_mb: 32
compactor:
  retention_enabled: true
  compaction_interval: 10m
```

### Cách hoạt động

Loki dùng filesystem storage trong volume `loki-data`, schema `boltdb-shipper`, index prefix `index_`, WAL `/loki/wal`. Compactor bật retention để xóa log cũ sau 7 ngày.

### Health check

```bash
curl -fsS http://localhost:3100/ready
```

### Logs

```bash
docker compose logs loki -f --tail=100
```

### Restart / reload

```bash
docker compose restart loki
```

## promtail

**Image:** `grafana/promtail:2.9.0`  
**Role:** Đọc file log và push sang Loki.  
**Port:** internal `9080`

### Cấu hình quan trọng

```yaml
clients:
  - url: http://loki:3100/loki/api/v1/push
scrape_configs:
  - job_name: spring-app
    labels:
      __path__: /logs/app/application.log
```

### Cách hoạt động

Promtail mount `./logs:/logs:ro`, parse JSON cho Spring/Nginx logs, gắn labels `job`, `level`, `logger`, `status`, `method`, và drop health check spam.

### Health check

```bash
docker compose logs promtail --tail=20
```

### Logs

```bash
docker compose logs promtail -f --tail=100
```

### Restart / reload

```bash
docker compose restart promtail
```

## postgres-exporter

**Image:** `prometheuscommunity/postgres-exporter:v0.15.0`  
**Role:** Expose PostgreSQL metrics cho Prometheus.  
**Port:** internal `9187`

### Cấu hình quan trọng

```yaml
DATA_SOURCE_NAME: postgresql://${DB_USER:-appuser}:${DB_PASSWORD:-changeme_in_production}@postgres-primary:5432/${DB_NAME:-sebook}?sslmode=disable
```

### Cách hoạt động

Exporter connect primary và expose metrics về connections, locks, transactions, replication-related views.

### Health check

```bash
curl -fsS http://localhost:9090/api/v1/targets | grep postgres-exporter
```

### Logs

```bash
docker compose logs postgres-exporter -f --tail=100
```

### Restart / reload

```bash
docker compose restart postgres-exporter
```

## nginx-exporter

**Image:** `nginx/nginx-prometheus-exporter:1.3.0`  
**Role:** Chuyển `stub_status` của Nginx thành Prometheus metrics.  
**Port:** internal `9113`

### Cấu hình quan trọng

```yaml
command: ["--nginx.scrape-uri=http://nginx/nginx_status"]
```

### Cách hoạt động

Exporter gọi `http://nginx/nginx_status` qua Docker network. Nginx allow Docker subnet nên exporter đọc được status.

### Health check

```bash
curl -fsS http://localhost:9090/api/v1/targets | grep nginx-exporter
```

### Logs

```bash
docker compose logs nginx-exporter -f --tail=100
```

### Restart / reload

```bash
docker compose restart nginx-exporter
```

## redis-exporter

**Image:** `oliver006/redis_exporter:v1.62.0`  
**Role:** Expose Redis metrics cho Prometheus.  
**Port:** internal `9121`

### Cấu hình quan trọng

```yaml
environment:
  REDIS_ADDR: redis://redis:6379
```

### Cách hoạt động

Exporter connect Redis nội bộ và expose memory, keyspace, command stats, persistence metrics.

### Health check

```bash
curl -fsS http://localhost:9090/api/v1/targets | grep redis-exporter
```

### Logs

```bash
docker compose logs redis-exporter -f --tail=100
```

### Restart / reload

```bash
docker compose restart redis-exporter
```
