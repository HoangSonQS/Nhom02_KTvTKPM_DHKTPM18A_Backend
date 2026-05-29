# ADR-003: Loki instead of ELK

**Ngày:** 2026-05-29  
**Trạng thái:** Accepted  
**Người quyết định:** Team

## Bối cảnh

Hệ thống cần centralized logging để developer vận hành stack mà không phải SSH vào từng container và grep file log. Yêu cầu gồm JSON logs, query theo labels, dashboard/alert trong Grafana, và retention 7 ngày.

## Các lựa chọn đã xem xét

| Lựa chọn | Ưu | Nhược |
|----------|----|-------|
| Loki + Promtail | Nhẹ, tích hợp Grafana, labels phù hợp container logs, dễ chạy Compose | Full-text indexing không mạnh bằng Elasticsearch |
| ELK/Elastic Stack | Search mạnh, ecosystem lớn | Tốn RAM/CPU hơn, vận hành phức tạp hơn |
| OpenSearch | Tương tự Elasticsearch, open-source friendly | Vẫn nặng với scale hiện tại |
| Chỉ dùng file logs | Đơn giản, không thêm service | Khó query, khó alert, khó xem cross-service |

## Quyết định

Chọn Loki + Promtail. Promtail đọc:

- `/logs/app/application.log`
- `/logs/app/error.log`
- `/logs/app/audit.log`
- `/logs/nginx/access.log`
- `/logs/nginx/error.log`

Loki dùng filesystem storage, `boltdb-shipper`, retention `168h`, và compactor `retention_enabled: true`.

## Hệ quả

- Query tốt nhất khi có labels đúng: `job`, `level`, `logger`, `status`, `method`.
- Không nên gắn label cardinality cao như `traceId`; traceId nên tìm bằng text filter.
- Nếu log volume tăng lớn hoặc cần full-text search nâng cao, có thể cần OpenSearch/Elasticsearch sau này.
- Cần phân biệt retention của Loki với rotation của file log trên host; Nginx logs cần logrotate riêng nếu production chạy lâu.
