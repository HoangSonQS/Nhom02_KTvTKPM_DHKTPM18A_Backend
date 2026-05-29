# Hệ thống Logging & Monitoring

## Tổng quan luồng log

```text
Spring Boot App
  | ghi JSON vào /logs/app/application.log, error.log, audit.log
  v
Promtail
  | đọc file, multiline, parse JSON, gắn labels
  v
Loki
  | filesystem storage + boltdb-shipper index + retention 168h
  v
Grafana
  | Explore, dashboards, alert rules
```

Nginx cũng ghi JSON access log vào `/var/log/nginx/access.log`, được bind mount về `./logs/nginx/access.log` và Promtail đọc qua `/logs/nginx/access.log`.

## Log files

| File | Nội dung | Retention |
|------|----------|-----------|
| `/logs/app/application.log` | Tất cả root `INFO+` của Spring Boot | Rolling 30 ngày, 50MB/file, tổng 2GB |
| `/logs/app/error.log` | Log `ERROR+` qua `ThresholdFilter` | Rolling 90 ngày, tổng 500MB |
| `/logs/app/audit.log` | Logger riêng `AUDIT` | Rolling 365 ngày, tổng 1GB |
| `/logs/nginx/access.log` | HTTP requests JSON từ Nginx | Không có rotate riêng trong repo; nên bổ sung logrotate nếu production host chạy lâu |
| `/logs/nginx/error.log` | Nginx error level `warn` | Không có rotate riêng trong repo; nên bổ sung logrotate nếu production host chạy lâu |

Loki có retention riêng `168h` trong `monitoring/loki/loki-config.yml`. Retention này xóa dữ liệu đã ingest vào Loki, không thay thế log rotation trên host files.

## Cấu trúc 1 log line Spring Boot

Logback dùng `net.logstash.logback.encoder.LogstashEncoder`. Ví dụ:

```json
{
  "@timestamp": "2026-05-29T09:19:36.928998261Z",
  "@version": "1",
  "message": "Initializing Spring DispatcherServlet 'dispatcherServlet'",
  "logger_name": "org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/]",
  "thread_name": "http-nio-8080-exec-1",
  "level": "INFO",
  "level_value": 20000,
  "app": "SEBook-Backend",
  "env": "prod"
}
```

Khi request được trace, log có thể có `traceId` và `spanId` qua Micrometer/Brave MDC. Sampling mặc định:

```properties
management.tracing.sampling.probability=${MANAGEMENT_TRACING_SAMPLING_PROBABILITY:0.1}
management.zipkin.tracing.export.enabled=${ZIPKIN_TRACING_EXPORT_ENABLED:false}
```

Zipkin export mặc định tắt để không spam warning khi stack chưa có Zipkin collector.

## Nginx JSON log

Nginx log format:

```json
{
  "time": "$time_iso8601",
  "status": "$status",
  "method": "$request_method",
  "uri": "$request_uri",
  "upstream": "$upstream_addr",
  "response_time": "$request_time",
  "upstream_response_time": "$upstream_response_time",
  "body_bytes": "$body_bytes_sent",
  "remote_addr": "$remote_addr",
  "request_id": "$request_id",
  "user_agent": "$http_user_agent"
}
```

`request_id` được forward sang app bằng header `X-Request-Id`.

## Promtail labels

| Job | Path | Labels chính |
|-----|------|--------------|
| `spring-app` | `/logs/app/application.log` | `env`, `level`, `logger` |
| `spring-error` | `/logs/app/error.log` | `severity=error`, `level` |
| `spring-audit` | `/logs/app/audit.log` | `job=spring-audit` |
| `nginx-access` | `/logs/nginx/access.log` | `status`, `method` |
| `nginx-error` | `/logs/nginx/error.log` | `severity=error` |

Promtail drop các dòng health check để giảm noise:

```yaml
- drop:
    expression: '.*health.*'
```

## Cách query log trên Grafana

Truy cập `http://localhost:3000` -> Explore -> chọn datasource `Loki`.

```logql
# Tất cả log của app
{job="spring-app"}

# Chỉ ERROR
{job="spring-error"}

# Tìm theo traceId nếu log có traceId
{job="spring-app"} |= "65d5b9a1c3f4e2b1"

# Request Nginx status 5xx
{job="nginx-access", status=~"5.."}

# Request chậm trên Nginx lớn hơn 1 giây
{job="nginx-access"} | json | response_time > 1

# Đếm error theo cửa sổ 5 phút
sum(count_over_time({job="spring-error"}[5m]))
```

## Metrics - Prometheus + Grafana

Prometheus scrape mỗi 15 giây:

| Job | Target |
|-----|--------|
| `spring-boot` | `app-1:8080`, `app-2:8080` tại `/actuator/prometheus` |
| `nginx` | `nginx-exporter:9113` |
| `postgres` | `postgres-exporter:9187` |
| `redis` | `redis-exporter:9121` |
| `prometheus` | `localhost:9090` |

### Metrics quan trọng

| Metric | Ý nghĩa | Alert khi |
|--------|---------|-----------|
| `jvm_memory_used_bytes` | JVM memory usage | Heap gần đầy, cần so với `jvm_memory_max_bytes` |
| `http_server_requests_seconds` | HTTP latency/count | p99 tăng cao hoặc error rate cao |
| `hikaricp_connections_active` | Active DB connections | Gần bằng maximum pool size |
| `pg_stat_activity_count` | Số connection PostgreSQL | Tăng bất thường |
| `redis_memory_used_bytes` | Redis memory | Gần 256MB |
| `nginx_connections_active` | Active Nginx connections | Spike bất thường |

### Dashboards có sẵn

| Dashboard file | Dữ liệu |
|----------------|---------|
| `monitoring/grafana/dashboards/spring-boot.json` | JVM, HTTP, actuator metrics |
| `monitoring/grafana/dashboards/nginx.json` | Nginx connections/requests |
| `monitoring/grafana/dashboards/postgres.json` | PostgreSQL metrics |
| `monitoring/grafana/dashboards/redis.json` | Redis memory/keyspace |

Grafana provision dashboards từ:

```yaml
path: /var/lib/grafana/dashboards
foldersFromFilesStructure: true
```

## Alert rules

| Alert | Điều kiện | Severity | File |
|-------|-----------|----------|------|
| `ERROR spike detected` | `sum(count_over_time({job="spring-error"}[5m])) > 10` trong 1 phút | `warning` | `monitoring/grafana/provisioning/alerting/rules.yml` |

Alert query sẽ tạo log `executing query` trong Loki mỗi phút. Đây là hành vi bình thường nếu status query là `200`.

## Lệnh verify nhanh

```bash
docker compose logs loki --tail=100
docker compose logs promtail --tail=100
curl -fsS http://localhost:3100/ready
curl -fsS http://localhost:9090/-/healthy
curl -fsS http://localhost:3000/api/health
```
