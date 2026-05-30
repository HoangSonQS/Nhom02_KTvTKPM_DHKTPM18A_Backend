# Chay Docker Stack Bang Code Backend Local

File nay dung cho dev muon chay toan bo Docker stack nhung backend app duoc build tu source local, khong pull image backend tu Docker Hub.

## Chuan bi code local

```powershell
cd D:\HK2-2025-2026\KTvTKPM\Nhom02_KTvTKPM_DHKTPM18A\Nhom02_KTvTKPM_DHKTPM18A_Backend

git fetch origin
git checkout develop
git pull origin develop
```

## Chay toan bo Docker stack bang backend local

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --build
```

Lenh tren se:

- Chay toan bo stack trong `docker-compose.yml`.
- Override service `app-1` va `app-2` bang `docker-compose.local.yml`.
- Build backend tu `Dockerfile` local thanh image `sebook-backend:local`.
- Khong pull backend image `${DOCKERHUB_USERNAME}/sebook-backend` tu Docker Hub.

## Moi lan sua code backend local

Sau khi sua code Java, Docker khong tu nap code moi vao container dang chay. Can build lai image local va recreate container app:

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml down

docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --build --force-recreate app-1 app-2 nginx
```

Neu chi muon rebuild backend, khong can down ca stack:

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml build app-1
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --force-recreate app-1 app-2 nginx
```

Neu nghi Docker dang dung cache cu:

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml build --no-cache app-1
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --force-recreate app-1 app-2 nginx
```

## Xem log backend

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml logs -f --tail=200 app-1 app-2
```

## Build lai backend sach tu source local

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml build --no-cache app-1
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d
```

## Restart backend app

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml restart app-1 app-2
```

## Dung toan bo stack

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml down
```

## Luu y

- Khong chay `git pull` trong container.
- Pull code tren may host/local truoc, sau do moi build Docker.
- Service backend trong compose la `app-1` va `app-2`, khong phai `backend`.
