# Database - PostgreSQL Replication

## Kiến trúc

```text
                    write
App-1/App-2  -----------------> postgres-primary
   |                              |
   | read                         | WAL streaming
   +---------> postgres-replica-1 | slot replica_1
   |                              |
   +---------> postgres-replica-2 | slot replica_2
```

Trong `docker-compose.yml`, app mặc định dùng `DB_URL=jdbc:postgresql://postgres-primary:5432/${DB_NAME:-sebook}`. File `spring/datasource.yml` mô tả cấu hình read replicas qua `DB_READ_HOST_1` và `DB_READ_HOST_2`; Java routing datasource cần được wire vào application nếu muốn route read/write ở runtime.

## Replication hoạt động như thế nào

Primary bật các setting:

```conf
wal_level = replica
max_wal_senders = 3
max_replication_slots = 3
wal_keep_size = 64MB
hot_standby = on
```

Khi primary init database lần đầu, `postgres/primary/init-replication-user.sh`:

1. Tạo role replication `${POSTGRES_REPLICATION_USER}` nếu chưa tồn tại.
2. Tạo physical replication slot `replica_1`.
3. Tạo physical replication slot `replica_2`.

Mỗi replica chạy `postgres/replica/init-replica.sh`:

1. Đợi primary ready bằng `pg_isready`.
2. Nếu data directory rỗng, chạy `pg_basebackup -R --slot=<slot>`.
3. Ghi `primary_conninfo`, `primary_slot_name`, `hot_standby = on`.
4. Tạo `standby.signal`.
5. Đảm bảo `max_connections = 200` trên replica để khớp primary.

`max_connections` trên standby phải lớn hơn hoặc bằng primary. Nếu replica có `max_connections` thấp hơn primary, PostgreSQL có thể fail recovery/standby vì config không tương thích với WAL từ primary.

## Các lệnh thường dùng

### Kiểm tra trạng thái replication

```bash
docker compose exec postgres-primary psql -U ${DB_USER:-appuser} -d ${DB_NAME:-sebook} -c "SELECT client_addr, state, (sent_lsn - replay_lsn) AS lag_bytes FROM pg_stat_replication;"
```

Kết quả tốt:

```text
state = streaming
lag_bytes < 1048576
```

### Kiểm tra replication slots

```bash
docker compose exec postgres-primary psql -U ${DB_USER:-appuser} -d ${DB_NAME:-sebook} -c "SELECT slot_name, active, restart_lsn FROM pg_replication_slots ORDER BY slot_name;"
```

`active = t` nghĩa là replica đang kết nối slot đó.

### Kiểm tra replica có đang sync không

```bash
docker compose exec postgres-replica-1 psql -U ${POSTGRES_REPLICATION_USER:-replicator} -d postgres -c "SELECT now() - pg_last_xact_replay_timestamp() AS replication_delay;"
```

Nếu hệ thống đang có write traffic, delay dưới 1 giây là tốt. Nếu không có transaction mới, giá trị có thể lớn hơn vì replica không có WAL mới để replay.

### Connect trực tiếp

```bash
docker compose exec postgres-primary psql -U ${DB_USER:-appuser} -d ${DB_NAME:-sebook}
docker compose exec postgres-replica-1 psql -U ${DB_USER:-appuser} -d ${DB_NAME:-sebook}
docker compose exec postgres-replica-2 psql -U ${DB_USER:-appuser} -d ${DB_NAME:-sebook}
```

### Chạy script check có sẵn

```bash
make check-replication
```

Script này in replication slots, sender state trên primary, và WAL receiver state trên từng replica.

## Backup & Restore

### Backup

```bash
docker compose exec postgres-primary pg_dump -U ${DB_USER:-appuser} ${DB_NAME:-sebook} | gzip > backup_$(date +%Y%m%d).sql.gz
```

### Restore

```bash
gunzip -c backup_20260529.sql.gz | docker compose exec -T postgres-primary psql -U ${DB_USER:-appuser} -d ${DB_NAME:-sebook}
```

Sau restore lớn, theo dõi replicas vì WAL có thể tăng nhanh:

```bash
make check-replication
```

## Slow query log

Primary log query chạy lâu hơn 1 giây:

```conf
log_min_duration_statement = 1000
log_statement = 'none'
log_duration = off
log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h '
```

Lý do không bật `log_statement = 'all'`: production sẽ rất ồn, tốn disk, và có rủi ro log dữ liệu nhạy cảm.

## Xử lý sự cố

### Replica bị out of sync

```bash
docker compose stop postgres-replica-1
docker volume rm nhom02_ktvtkpm_dhktpm18a_backend_postgres-read-1-data
docker compose up -d postgres-replica-1
docker compose logs postgres-replica-1 -f --tail=100
```

Chờ log:

```text
started streaming WAL from primary
```

Tên volume có thể khác nếu Compose project name khác. Kiểm tra bằng:

```bash
docker volume ls | grep postgres-read-1-data
```

### Lỗi `database "replicator" does not exist`

Nguyên nhân thường gặp là `pg_isready -U replicator` không chỉ rõ database, PostgreSQL sẽ mặc định connect database tên `replicator`. Stack hiện tại đã fix healthcheck replica bằng:

```yaml
pg_isready -U ${POSTGRES_REPLICATION_USER:-replicator} -d postgres
```

Nếu log này xuất hiện lại, kiểm tra `docker-compose.yml` và recreate replicas:

```bash
docker compose up -d --force-recreate postgres-replica-1 postgres-replica-2
```

### `max_connections` mismatch

Triệu chứng: replica fail start/recovery sau khi primary đổi `max_connections`. Fix bằng cách đặt cùng giá trị trên replica. Script hiện tại tự ghi:

```sh
max_connections = 200
```

vào `postgresql.auto.conf` của replica. Nếu đổi primary lên giá trị khác, update cả `postgres/replica/init-replica.sh` và bootstrap lại replicas nếu cần.

### Replication lag tăng

```bash
docker compose exec postgres-primary psql -U ${DB_USER:-appuser} -d ${DB_NAME:-sebook} -c "SELECT application_name, client_addr, state, write_lag, flush_lag, replay_lag FROM pg_stat_replication;"
```

Hướng xử lý:

- Kiểm tra replica có restart liên tục không: `docker compose ps`.
- Kiểm tra disk/IO trên host.
- Giảm batch write lớn nếu application đang import dữ liệu.
- Đảm bảo `wal_keep_size` và replication slots không bị giữ WAL quá lâu làm đầy disk.
