# ADR-001: Nginx as API Gateway

**Ngày:** 2026-05-29  
**Trạng thái:** Accepted  
**Người quyết định:** Team

## Bối cảnh

Hệ thống cần một entry point ổn định cho client, có khả năng reverse proxy đến nhiều Spring Boot instances, rate limit, log HTTP request và expose endpoint health. Stack được deploy bằng Docker Compose, ưu tiên giải pháp nhẹ và dễ vận hành.

## Các lựa chọn đã xem xét

| Lựa chọn | Ưu | Nhược |
|----------|----|-------|
| Nginx | Nhẹ, battle-tested, cấu hình đơn giản, không cần thêm JVM | Không có circuit breaker/routing động như gateway app |
| Spring Cloud Gateway | Java native, filter/circuit breaker tốt, dễ tích hợp Spring ecosystem | Thêm một JVM, nặng hơn, cần code/config phức tạp hơn |
| Kong | Plugin phong phú, observability tốt | Overkill cho Compose stack, cần thêm operational knowledge |
| Traefik | Auto-discovery Docker tốt, TLS automation | Cần thay đổi cách routing/labels, chưa cần cho upstream explicit |

## Quyết định

Chọn Nginx làm API Gateway và Load Balancer. Config hiện tại dùng:

```nginx
upstream springboot_backend {
    least_conn;
    server app-1:8080 max_fails=3 fail_timeout=30s;
    server app-2:8080 max_fails=3 fail_timeout=30s;
    keepalive 32;
}
```

Nginx cũng áp dụng rate limit `10r/s` theo IP, JSON access log, security headers và `/nginx_status` cho exporter.

## Hệ quả

- Thêm/sửa app instance phải update `nginx/conf.d/default.conf`.
- Nếu cần circuit breaker nâng cao, retry theo business rule, hoặc service discovery động, có thể cần gateway khác trong tương lai.
- Đổi lại, stack hiện tại nhẹ, dễ debug, và phù hợp quy mô 2 app instances.
