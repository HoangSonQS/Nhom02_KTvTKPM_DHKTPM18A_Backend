#!/usr/bin/env bash
set -euo pipefail

APP_DIR="/opt/app"
REPO_URL="https://github.com/xuantruong121/Nhom02_KTvTKPM_DHKTPM18A_Backend.git"
APP_USER="${APP_USER:-ubuntu}"

echo "=== Preparing EC2 host for SEBook CI/CD ==="

if ! command -v git >/dev/null 2>&1; then
  sudo apt-get update
  sudo apt-get install -y git
fi

if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sudo sh
  sudo usermod -aG docker "$APP_USER"
fi

sudo mkdir -p "$APP_DIR"
sudo chown -R "$APP_USER:$APP_USER" "$APP_DIR"

if [ ! -d "$APP_DIR/.git" ]; then
  rm -rf "$APP_DIR"
  git clone "$REPO_URL" "$APP_DIR"
fi

cd "$APP_DIR"
git fetch origin main
git checkout main
git pull origin main

if [ ! -f .env ]; then
  cp .env.example .env
fi

mkdir -p logs/nginx logs/app backups config/keys

echo ""
echo "=== QUAN TRỌNG: Điền giá trị thật vào /opt/app/.env ==="
echo "nano /opt/app/.env"
echo ""
echo "Đảm bảo .env có ít nhất:"
echo "DOCKERHUB_USERNAME=<dockerhub-username>"
echo "APP_VERSION=latest"
echo ""

docker compose pull postgres-primary postgres-replica-1 postgres-replica-2 redis prometheus grafana loki promtail postgres-exporter nginx-exporter redis-exporter
docker compose up -d postgres-primary postgres-replica-1 postgres-replica-2 redis prometheus grafana loki promtail postgres-exporter redis-exporter

echo ""
echo "=== Setup xong. Chờ GitHub Actions deploy app-1/app-2 và start nginx ==="
echo "Sau khi cập nhật .env, kiểm tra bằng: docker compose ps"
