# Docker Deployment

The frontend/Nginx and Spring Boot backend run in Docker. MySQL is an external
service and is not created or managed by Docker Compose.

For the production runbook—including external MySQL setup, schema initialization,
firewall requirements, HTTPS, backups, and troubleshooting—see
[`../../DEPLOYMENT.md`](../../DEPLOYMENT.md).

## Quick start

1. Prepare a production `.env` based on `.env.example`. Set `DB_URL`,
   `DB_USERNAME`, and `DB_PASSWORD` to credentials and an address reachable
   from the backend container.
2. Ensure the external MySQL database and tables are ready.
3. Build and start:

```sh
docker compose up -d --build
```

The frontend/Nginx container publishes host port 80. Stop the application with:

```sh
docker compose down
```
