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

## Production Deployment

The frontend/Nginx and Spring Boot backend run in Docker; MySQL is an external
service. Follow [`DEPLOYMENT.md`](DEPLOYMENT.md) for database preparation,
production configuration, HTTPS, startup, and operational checks.
