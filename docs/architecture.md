# Kiến trúc hệ thống

## Tổng quan

SEBook Backend là ứng dụng Spring Boot modular monolith. Stack production hiện tại tách các trách nhiệm vận hành thành nhiều service: Nginx làm gateway/load balancer, hai instance Spring Boot để tăng khả năng sẵn sàng, PostgreSQL primary/replica để tách write/read, Redis làm cache, và monitoring stack gồm Prometheus, Grafana, Loki, Promtail.

Kiến trúc này phù hợp giai đoạn scale vừa: dễ chạy bằng Docker Compose, ít thành phần phức tạp, nhưng đã có healthcheck, metrics, log aggregation, database replication và runbook vận hành.

## Sơ đồ kiến trúc

```text
Internet
   |
   v
+------------------------------------------------+
| Nginx :80/:443                                 |
| API Gateway + Load Balancer + Rate Limit       |
| upstream springboot_backend, least_conn        |
+----------------------+-------------------------+
                       |
          +------------+------------+
          |                         |
          v                         v
   +-------------+           +-------------+
   | app-1       |           | app-2       |
   | Spring Boot |           | Spring Boot |
   | :8080       |           | :8080       |
   | host :8080  |           | host :8081  |
   +------+------+           +------+------+
          |                         |
          +------------+------------+
                       |
        +--------------+-------------------------+
        |              |                         |
        v              v                         v
+---------------+  +------------------+  +------------------+
| postgres-     |  | postgres-        |  | redis            |
| primary       |  | replica-1        |  | :6379 internal   |
| write :5432   |  | read :5432       |  | cache/AOF        |
+-------+-------+  +------------------+  +------------------+
        |
        | WAL streaming via physical slots
        v
+------------------+
| postgres-        |
| replica-2        |
| read :5432       |
+------------------+

Monitoring network
------------------
Prometheus :9090 scrapes:
  - app-1:8080/actuator/prometheus
  - app-2:8080/actuator/prometheus
  - nginx-exporter:9113
  - postgres-exporter:9187
  - redis-exporter:9121

Promtail reads ./logs -> Loki :3100 -> Grafana :3000
Grafana reads Prometheus + Loki and provisions dashboards/alerts.
```

## Luồng request

1. Client gửi request đến `http://localhost/api/...` hoặc domain production trỏ về Nginx.
2. Nginx áp dụng security headers và rate limit `10r/s` theo IP, cho burst `20`.
3. Nginx dùng upstream `springboot_backend` với strategy `least_conn`.
4. Request được forward đến `app-1:8080` hoặc `app-2:8080`, kèm các header `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto`, `X-Request-Id`.
5. Spring Boot xử lý authentication, validation và business logic.
6. Nếu truy vấn write, datasource mặc định trỏ đến `postgres-primary`.
7. Nếu ứng dụng tích hợp routing read/write theo `spring/datasource.yml`, read query có thể đi đến `postgres-replica-1` hoặc `postgres-replica-2`.
8. Nếu dữ liệu đã có trong cache, Spring Boot đọc Redis và bỏ qua DB.
9. Response quay lại qua Nginx. Nginx ghi JSON access log vào `./logs/nginx/access.log`.
10. Spring Boot ghi JSON log vào `./logs/app/*.log`; Promtail đọc file và push sang Loki.

## Quyết định thiết kế quan trọng

| Quyết định | Lý do |
|------------|-------|
| Nginx thay vì Spring Cloud Gateway | Nhẹ, quen thuộc, không cần thêm JVM, phù hợp Compose stack |
| Hai service `app-1`/`app-2` riêng | Nginx upstream explicit, dễ debug và rolling restart từng instance |
| PostgreSQL streaming replication | Tách write/read bằng native PostgreSQL, không thêm middleware |
| Physical replication slots | Replica không mất WAL khi restart ngắn; slot `replica_1`, `replica_2` được tạo lúc init primary |
| Redis AOF + LRU | Cache có persistence cơ bản, giới hạn RAM `256mb`, tránh đầy memory |
| Prometheus + exporters | Chuẩn để scrape metrics từ app và infrastructure |
| Loki + Promtail thay vì ELK | Ít RAM hơn, tích hợp Grafana, phù hợp log file JSON |
| Grafana provisioning | Datasource, dashboard, alert được version trong Git, dễ reproduce mọi môi trường |

## Network boundaries

| Network | Service tham gia | Mục đích |
|---------|------------------|----------|
| `frontend` | `nginx` | Entry point từ host/internet |
| `backend` | `nginx`, apps, Postgres, Redis, exporters | Traffic API và data layer |
| `monitoring` | Prometheus, Grafana, Loki, Promtail, exporters | Metrics/logs/alerting |

## Giới hạn hiện tại

- HTTPS port `443` đã expose nhưng repo chưa có certificate/TLS server block.
- `app-1` và `app-2` đang map host ports `8080/8081` để test trực tiếp. Production nên giới hạn bằng firewall/VPN hoặc bỏ public mapping.
- `make scale-app` không scale động. Nếu thêm app instance cần thêm service và update `nginx/conf.d/default.conf`.
