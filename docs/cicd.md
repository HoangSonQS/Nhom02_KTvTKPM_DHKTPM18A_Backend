# CI/CD Pipeline

## Tổng quan

```text
Push to main
  |
  v
GitHub Actions
  |-- Build WAR bằng Maven Wrapper
  |-- Build Docker image
  |-- Push image lên DockerHub
  |-- SSH vào EC2
        |
        v
      docker compose pull app-1 app-2
      docker compose up -d --no-deps --force-recreate app-1 app-2
      docker compose up -d nginx
      health check qua http://localhost/actuator/health
```

## Cách hoạt động

- Mỗi lần push lên branch `main` sẽ tự động build và deploy.
- Workflow cũng hỗ trợ deploy thủ công qua `workflow_dispatch`.
- Docker image được push lên DockerHub với 2 tags:
  - `latest`
  - `${{ github.sha }}`
- EC2 dùng `APP_VERSION=${{ github.sha }}` để pull đúng image của commit vừa build.
- Pipeline chỉ recreate `app-1` và `app-2`; database, Redis, Prometheus, Grafana, Loki và Promtail không bị restart.
- Nếu health check fail, GitHub Actions báo lỗi và in logs của `app-1`, `app-2`, `nginx`.

## File liên quan

| File | Vai trò |
|------|---------|
| `.github/workflows/deploy.yml` | GitHub Actions pipeline |
| `Dockerfile` | Multi-stage Docker build |
| `docker-compose.yml` | Chạy stack trên EC2 bằng image DockerHub |
| `.env.example` | Template biến môi trường |
| `scripts/setup-ec2-cicd.sh` | Script setup EC2 lần đầu |

## Setup lần đầu trên EC2

SSH vào EC2:

```bash
ssh -i sebook-demo-key.pem ubuntu@54.254.6.166
```

Chạy script setup:

```bash
curl -fsSL https://raw.githubusercontent.com/xuantruong121/Nhom02_KTvTKPM_DHKTPM18A_Backend/main/scripts/setup-ec2-cicd.sh -o setup-ec2-cicd.sh
chmod +x setup-ec2-cicd.sh
./setup-ec2-cicd.sh
```

Sau khi script chạy xong, cập nhật secrets/runtime values:

```bash
nano /opt/app/.env
```

Các biến tối thiểu cần kiểm tra trong `/opt/app/.env`:

```env
DOCKERHUB_USERNAME=your-dockerhub-username
APP_VERSION=latest
SPRING_PROFILES_ACTIVE=prod
DB_PASSWORD=change_me
POSTGRES_REPLICATION_PASSWORD=change_me_replication
GRAFANA_PASSWORD=change_me_admin
JWT_PRIVATE_KEY=file:./config/keys/private_key.pem
JWT_PUBLIC_KEY=file:./config/keys/public_key.pem
```

JWT keys phải tồn tại trong:

```text
/opt/app/config/keys/private_key.pem
/opt/app/config/keys/public_key.pem
```

## GitHub Secrets cần thêm

Vào GitHub repo -> Settings -> Secrets and variables -> Actions -> New repository secret.

| Secret | Giá trị | Lấy ở đâu |
|--------|---------|-----------|
| `DOCKERHUB_USERNAME` | Tên tài khoản DockerHub | DockerHub profile |
| `DOCKERHUB_TOKEN` | Access token | DockerHub -> Account settings -> Security |
| `EC2_HOST` | `54.254.6.166` | AWS Console |
| `EC2_SSH_KEY` | Nội dung file `.pem` | File `sebook-demo-key.pem` |

### Cách lấy nội dung file `.pem` cho `EC2_SSH_KEY`

Chạy trên Windows PowerShell:

```powershell
Get-Content D:\Downloads\sebook-demo-key.pem
```

Copy toàn bộ output từ:

```text
-----BEGIN RSA PRIVATE KEY-----
```

đến:

```text
-----END RSA PRIVATE KEY-----
```

bao gồm cả hai dòng đầu cuối.

## Theo dõi pipeline

1. Mở GitHub repo.
2. Vào tab Actions.
3. Chọn workflow `Deploy to EC2`.
4. Mở run mới nhất để xem từng job: `build-and-push`, `deploy`.

## Trigger deploy thủ công

1. GitHub repo -> Actions.
2. Chọn `Deploy to EC2`.
3. Chọn `Run workflow`.
4. Chọn branch `main`.
5. Bấm `Run workflow`.

## Kiểm tra sau deploy

Trên EC2:

```bash
cd /opt/app
docker compose ps
curl -fsS http://localhost/actuator/health
docker compose logs app-1 app-2 --tail=100
```

Từ máy cá nhân:

```bash
curl -fsS http://54.254.6.166/actuator/health
```

## Rollback nếu cần

SSH vào EC2:

```bash
ssh -i sebook-demo-key.pem ubuntu@54.254.6.166
```

Dùng image theo commit cũ:

```bash
cd /opt/app
APP_VERSION=<git-sha-cu> docker compose pull app-1 app-2
APP_VERSION=<git-sha-cu> docker compose up -d --no-deps --force-recreate app-1 app-2
curl -fsS http://localhost/actuator/health
```

Nếu rollback thành công, cập nhật `/opt/app/.env`:

```env
APP_VERSION=<git-sha-cu>
```

## Troubleshooting

### DockerHub login fail

Kiểm tra secrets:

- `DOCKERHUB_USERNAME` đúng tên DockerHub.
- `DOCKERHUB_TOKEN` là access token, không phải password đăng nhập.

### SSH vào EC2 fail

Kiểm tra:

- Security Group mở port `22` cho IP chạy GitHub Actions hoặc tạm thời `0.0.0.0/0`.
- `EC2_HOST=54.254.6.166`.
- `EC2_SSH_KEY` copy đủ private key, không thiếu dòng BEGIN/END.

### Health check fail

Trên EC2:

```bash
cd /opt/app
docker compose logs app-1 --tail=100
docker compose logs app-2 --tail=100
docker compose logs nginx --tail=100
docker compose ps
```

Các nguyên nhân thường gặp:

- `.env` thiếu secret production.
- JWT key file chưa có trong `/opt/app/config/keys`.
- Database chưa healthy.
- DockerHub image chưa pull được do `DOCKERHUB_USERNAME` sai.
