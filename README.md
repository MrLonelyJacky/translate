# DocBridge

DocBridge is now an image-focused OCR + translation MVP.

## Current Scope

- Input: `JPG` / `PNG`
- Flow: upload image -> OCR -> translation -> side-by-side preview
- Output: online preview only (no download file, no async task queue)

## Local Run (Nginx, no Docker)

- Frontend dev server: `5173`
- Backend API: `8090`
- Browser entry: `80` via Nginx

See `deploy/nginx/README.md` for full startup steps.

## Docker Deployment

Docker files are prepared for production-style deployment:

- `backend/Dockerfile`
- `frontend/Dockerfile`
- `docker-compose.yml`
- `deploy/nginx/prod.conf`

Quick start:

```powershell
cd C:\devlopEnv\project\translate
Copy-Item .env.example .env
# fill required secrets in .env
docker compose up -d --build
```

App entry: `http://localhost`

