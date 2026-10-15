---
name: security
description: Security baseline for the Todo App project — input validation, CORS, secrets/config hygiene, and what to defer until Phase 2 auth. Use whenever writing an endpoint that accepts input, configuring CORS, handling credentials/config, or reviewing code for security issues.
---

# Security — baseline for this project

This is a local learning project with no auth in Phase 1 — treat security guidance here as "don't build in bad habits," not "harden for production." Skip anything that's genuinely Phase-2-or-later (auth, per-user isolation) unless the roadmap step calls for it.

## Input validation (applies from Phase 1)
- Every request DTO gets Bean Validation (`@NotBlank`, `@Size`, etc.) — this is also the first line of defense against malformed/oversized input, not just a UX nicety.
- Never build a SQL query by string concatenation — Spring Data JPA method queries / `@Query` with named parameters avoid SQL injection by construction; don't introduce raw `JdbcTemplate` string-built queries.
- Validate path/query params too (e.g. an `id` that should be a positive Long) — a malformed param should produce a clean 400, not an unhandled exception leaking a stack trace to the client.

## CORS
Configure CORS explicitly and narrowly (`config/CorsConfig.java`) — allow only the known frontend origin (`http://localhost:5173` in dev), not `*`. A wildcard origin is a habit that's easy to forget to tighten later.

## Secrets & config
- No credentials, API keys, or connection strings with real values committed to `application.yml`. Local dev placeholder values matching `docker-compose.yml` are fine (see `devops` skill); anything beyond that goes in an untracked `application-local.yml` or environment variables.
- If a `.env` or credentials file is ever added, it must be gitignored — check before committing when infra changes.

## Error responses
The `@RestControllerAdvice` error format should return a message and status, not the raw exception/stack trace — leaking stack traces in API responses is a common accidental info-disclosure bug even in local projects, and it's a habit worth not forming.

## Dependencies
Don't add dependencies casually — each one is attack surface. Prefer what Spring Boot Initializr already provides over adding a new library for something the standard library or an existing dependency already covers.

## Deferred to Phase 2 (don't build early)
- Authentication/authorization (Spring Security + JWT) — only when the roadmap step is reached.
- Per-user data isolation — meaningless before auth exists; don't add a `userId` column speculatively.
- Rate limiting — only relevant once the app is reachable beyond localhost.

## Do NOT
- Don't add Spring Security "just in case" before the auth step — it changes request handling for every existing endpoint and is a bigger change than it looks.
- Don't disable CSRF/CORS broadly to "make it work" — fix the actual origin/method mismatch instead.
- Don't log full request bodies or entities that might later contain user data, as a default logging habit.