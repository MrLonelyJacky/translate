# Nginx Usage

## Local Development (No Docker)

This setup routes all browser requests through Nginx:
- `/` -> Vite dev server (`http://127.0.0.1:5173`)
- `/api/` -> Spring Boot backend (`http://127.0.0.1:8090`)

### 1) Start backend

```powershell
cd C:\devlopEnv\project\translate\backend
$env:SPRING_PROFILES_ACTIVE="local"
mvn spring-boot:run
```

### 2) Start frontend dev server

```powershell
cd C:\devlopEnv\project\translate\frontend
pnpm dev --port 5173
```

### 3) Install and configure Nginx on Windows

- Download Nginx for Windows from nginx.org.
- Copy `deploy/nginx/local-dev.conf` into your Nginx `conf` directory.
- Include it from `nginx.conf` inside the `http {}` block, for example:

```nginx
include       mime.types;
default_type  application/octet-stream;

# ...existing config...
include C:/devlopEnv/project/translate/deploy/nginx/local-dev.conf;
```

- Reload Nginx:

```powershell
cd <your-nginx-home>
.\nginx.exe -s reload
```

### 4) Open app

Visit:
- `http://localhost`

Do not open `http://localhost:5173` directly when using this flow.

## Production

Use `deploy/nginx/prod.conf` as a base. It serves static files and proxies `/api/` to backend service `backend:8090`.

