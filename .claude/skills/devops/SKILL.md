---
name: devops
description: Local infra conventions for the Todo App project — docker-compose (Postgres + Redis), environment config, running the stack. Use whenever editing docker-compose.yml, application.yml datasource/cache config, or helping the user get the app running locally.
---

# DevOps — local infra conventions

Scope for this project is deliberately narrow: **local dev infra only** (Docker Compose). No CI/CD, no Kubernetes, no cloud deployment — that's out of scope for both Phase 1 and Phase 2 per `CLAUDE.md`. Don't introduce it unless the user explicitly asks.

## docker-compose.yml
Two services for now:
- `postgres` — Phase 1 onward. Pin a specific major version tag (e.g. `postgres:16`), not `latest` — reproducibility matters more than always-newest for a learning project.
- `redis` — Phase 2 onward only. Don't add it to compose until the Redis cache step actually starts.

Use named volumes for Postgres data (`postgres_data:/var/lib/postgresql/data`) so `docker-compose down` doesn't silently wipe local data — only `docker-compose down -v` should do that, and that's a deliberate, confirmable action.

## Environment config
`backend/src/main/resources/application.yml` holds datasource/Redis connection settings. For local dev, plain values matching docker-compose defaults are fine (this isn't a deployed service handling real secrets). If credentials ever stop being placeholder values, move them to environment variables / `application-local.yml` (gitignored) instead of committing them.

## Running the stack
Standard order:
```bash
docker-compose up -d        # postgres (+ redis in Phase 2)
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```
If the backend fails to connect on first run, it's almost always compose not being up yet or a port already in use (5432/6379) — check `docker ps` before assuming a code bug.

## Changing compose
When adding a new service or changing a port/volume, verify it doesn't collide with anything already running locally, and prefer `docker-compose down` (not `-v`) when testing changes so existing data survives the restart.

## Do NOT
- Don't add a Dockerfile for the Spring Boot app or a `production` compose profile — this project doesn't deploy anywhere yet.
- Don't add CI (GitHub Actions etc.) unless asked.
- Don't run `docker-compose down -v` or otherwise drop volumes without flagging it first — that's destructive to local dev data.