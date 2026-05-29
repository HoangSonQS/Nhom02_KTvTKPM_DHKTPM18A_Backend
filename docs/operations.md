# Vận hành hệ thống

## Lệnh thường dùng hằng ngày

```bash
make ps
docker compose ps

make logs
docker compose logs -f nginx
docker compose logs -f app-1 app-2

make health
make check-replication
```

## Khởi động và dừng stack

```bash
cp .env.example .env
# Điền password/key thật vào .env
make up
make ps
```

Dừng stack:

```bash
make down
```

Dừng và xóa volumes:

```bash
make clean
```

Cẩn thận với `make clean`: lệnh này xóa database, Redis, Prometheus, Grafana, Loki volumes.

## Deploy phiên bản mới

### Build cả hai app

```bash
docker compose build app-1 app-2
```

### Rolling update với 2 instances

```bash
docker compose up -d --no-deps app-1
curl -fsS http://localhost:8080/actuator/health
curl -fsS http://localhost/actuator/health

docker compose up -d --no-deps app-2
curl -fsS http://localhost:8081/actuator/health
curl -fsS http://localhost/actuator/health
```

Nếu Nginx trả 502 trong lúc deploy, xem `nginx/conf.d/default.conf` để đảm bảo upstream vẫn có ít nhất một app healthy.

## Reload config

```bash
# Validate compose
docker compose config --quiet

# Reload Nginx config không restart container
docker compose exec nginx nginx -t
docker compose exec nginx nginx -s reload

# Restart monitoring services sau khi đổi Loki/Promtail/Grafana config
docker compose restart loki promtail grafana

# Restart Postgres primary sau khi đổi postgresql.conf
docker compose restart postgres-primary
```

## Scale app

Stack hiện tại dùng `app-1` và `app-2` explicit trong Nginx upstream:

```nginx
server app-1:8080;
server app-2:8080;
```

Vì vậy `docker compose up -d --scale app=3` không áp dụng trực tiếp. Để thêm instance:

1. Duplicate service `app-3` trong `docker-compose.yml`.
2. Map host port mới nếu cần debug, ví dụ `8082:8080`.
3. Thêm `server app-3:8080` vào `nginx/conf.d/default.conf`.
4. Chạy:

```bash
docker compose up -d --build app-3
docker compose exec nginx nginx -s reload
```

## Runbook - Service không start

```bash
docker compose ps
docker compose logs <service> --tail=50
docker compose config --quiet
```

Kiểm tra port conflict trên Windows PowerShell:

```powershell
netstat -ano | findstr ":80"
netstat -ano | findstr ":3000"
```

Kiểm tra resource:

```bash
docker stats
docker system df
```

## Runbook - App trả về 502 Bad Gateway

```bash
docker compose ps app-1 app-2 nginx
docker compose logs nginx --tail=100
curl -fsS http://localhost:8080/actuator/health
curl -fsS http://localhost:8081/actuator/health
```

Nếu app direct OK nhưng Nginx fail:

```bash
docker compose exec nginx nginx -t
docker compose exec nginx wget -qO- http://app-1:8080/actuator/health
docker compose exec nginx wget -qO- http://app-2:8080/actuator/health
```

## Runbook - Database connection pool exhausted

Dấu hiệu: app log có `HikariPool` timeout.

```bash
docker compose exec postgres-primary psql -U ${DB_USER:-appuser} -d ${DB_NAME:-sebook} -c "SELECT count(*), state FROM pg_stat_activity GROUP BY state;"
```

Kill idle connections quá 10 phút nếu cần:

```bash
docker compose exec postgres-primary psql -U ${DB_USER:-appuser} -d ${DB_NAME:-sebook} -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state = 'idle' AND state_change < now() - interval '10 min';"
```

Sau đó xem app log:

```bash
docker compose logs app-1 app-2 --tail=100 | grep -i "hikari\|timeout\|exception"
```

## Runbook - Disk đầy vì logs

Kiểm tra trên host:

```bash
du -sh logs/*
du -sh logs/app/*
du -sh logs/nginx/*
docker system df
```

Logback tự rotate app logs, Loki tự retention log đã ingest. Nginx log file trong repo chưa có logrotate, nên production nên thêm host logrotate. Xóa thủ công log gzip cũ:

```bash
find logs/app -name "*.log.gz" -mtime +30 -delete
```

## Runbook - Loki log quá ồn

Grafana alert mỗi phút query Loki và có thể tạo log `executing query`. Nếu status `200`, đây là bình thường. Nếu muốn giảm noise, có thể thêm `log_level: warn` vào `monitoring/loki/loki-config.yml` phần `server`, sau đó:

```bash
docker compose restart loki
```

## Runbook - Promtail không đẩy log vào Loki

```bash
docker compose ps promtail loki
docker compose logs promtail --tail=100
curl -fsS http://localhost:3100/ready
```

Kiểm tra query:

```bash
curl -fsS "http://localhost:3100/loki/api/v1/query_range?query=%7Bjob%3D%22spring-app%22%7D&limit=3"
```

## Checklist trước khi go-live

- [ ] Đổi tất cả password/key mặc định trong `.env`.
- [ ] `SPRING_PROFILES_ACTIVE=prod`.
- [ ] JWT private/public keys có thật trong `./config/keys`.
- [ ] `docker compose config --quiet` pass.
- [ ] `make health` pass.
- [ ] `make check-replication` cho 2 replicas `streaming`.
- [ ] Grafana datasource Prometheus và Loki hoạt động.
- [ ] Alert `ERROR spike detected` được provision.
- [ ] Backup database lần đầu.
- [ ] Giới hạn port public: chỉ `80/443`; `3000/9090/3100/8080/8081` chỉ internal/VPN.
- [ ] Bổ sung TLS config cho Nginx nếu expose internet.
- [ ] Bổ sung logrotate cho `logs/nginx/*.log` trên host.
