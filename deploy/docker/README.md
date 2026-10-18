# Docker Deployment

This project uses three containers in production-like mode:

- `frontend` (Nginx serving Vue `dist`)
- `backend` (Spring Boot API)
- `mysql` (MySQL 8.4)

## 1. Prepare env file

Copy `.env.example` to `.env` and fill secrets:

```powershell
cd C:\devlopEnv\project\translate
Copy-Item .env.example .env
```

## 2. Build and start

```powershell
cd C:\devlopEnv\project\translate
docker compose up -d --build
```

## 3. Access

- App: `http://localhost`
- Backend health: `http://localhost/api/actuator/health`

## 4. Stop

```powershell
cd C:\devlopEnv\project\translate
docker compose down
```

## Notes

- Backend runs with `SPRING_PROFILES_ACTIVE=prod` in compose.
- API requests are proxied by `deploy/nginx/prod.conf` from `/api/` to `backend:8090`.
- Storage files are persisted in docker volume `backend-storage`.

