# ADR-002: PostgreSQL Streaming Replication

**Ngày:** 2026-05-29  
**Trạng thái:** Accepted  
**Người quyết định:** Team

## Bối cảnh

Ứng dụng cần database production-ready hơn single PostgreSQL container: read traffic có thể tăng, cần có replicas để giảm tải primary và tạo nền tảng cho failover/backup read-only. Stack vẫn cần chạy được bằng Docker Compose, không thêm quá nhiều middleware.

## Các lựa chọn đã xem xét

| Lựa chọn | Ưu | Nhược |
|----------|----|-------|
| Single PostgreSQL | Đơn giản nhất, ít config | Không tách read/write, SPOF rõ ràng |
| Native streaming replication | Native PostgreSQL, ít thành phần, có read replicas | Failover chưa tự động, cần quản lý slots/WAL |
| Patroni | HA/failover tốt, production-grade | Phức tạp hơn, cần etcd/Consul hoặc DCS |
| Pgpool-II | Read/write split và pooling | Thêm layer phức tạp, cần tuning và debug riêng |

## Quyết định

Chọn PostgreSQL native streaming replication với 1 primary và 2 replicas. Primary bật:

```conf
wal_level = replica
max_wal_senders = 3
max_replication_slots = 3
wal_keep_size = 64MB
```

Replica bootstrap bằng `pg_basebackup` và physical slots `replica_1`, `replica_2`.

## Hệ quả

- Read replicas có eventual consistency; đọc ngay sau write có thể có lag nhỏ.
- Failover chưa tự động. Nếu primary mất, team phải có runbook promote replica riêng trước khi go-live HA thật.
- Physical slots giữ WAL cho replica; nếu replica down lâu, primary disk có thể tăng. Cần monitor replication lag và disk usage.
- `max_connections` cần đồng bộ giữa primary và replicas.
