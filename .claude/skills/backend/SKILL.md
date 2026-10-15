---
name: backend
description: Spring Boot / Java conventions for the Todo App backend — layering, DTO mapping, validation, exception handling, JPA/Redis patterns. Use whenever writing or reviewing code under backend/src, or when unsure how a backend concept (entity, repository, service, controller) should be structured in this project.
---

# Backend — Spring Boot conventions

Reference this skill whenever touching `backend/`. It captures the *how* for this specific project; the *what to build next* lives in `todo-app-dev`.

## Layering (never skip a layer)
`Controller → Service → Repository → Entity`. Controllers never touch `Repository` directly, and entities are never returned from a controller — always map to a DTO.

```
controller/   REST endpoints, @Valid on request bodies, delegates to service
service/      business logic, transaction boundaries (@Transactional where needed)
repository/   Spring Data JPA interfaces only, no logic
entity/       @Entity classes, JPA annotations
dto/          TaskRequest / TaskResponse, one direction each
exception/    @RestControllerAdvice + custom exceptions (TaskNotFoundException...)
config/       CORS, Redis, security beans
```

## DTO mapping
Map entity ↔ DTO by hand (a small `toResponse()`/`toEntity()` method or a dedicated mapper class) rather than pulling in MapStruct/ModelMapper in Phase 1 — the goal is understanding what's happening, not hiding it behind a library. Revisit in Phase 2 if mapping code gets large.

## Validation
Bean Validation annotations on the request DTO (`@NotBlank`, `@Size`, `@Future`...). Only hand-write validation in the service when the rule can't be expressed declaratively (e.g. cross-field checks).

## Exception handling
One `@RestControllerAdvice` class. Each handled exception returns a consistent error shape:
```json
{ "status": 404, "message": "Task not found: 42", "timestamp": "..." }
```
Prefer custom unchecked exceptions (`TaskNotFoundException extends RuntimeException`) over generic ones so the advice can map them precisely to status codes.

## Persistence
- Phase 1: `spring.jpa.hibernate.ddl-auto=update` is fine for iterating.
- Phase 2+: once there's real data to preserve, switch to Flyway migrations (`src/main/resources/db/migration/V{n}__description.sql`) — `ddl-auto` becomes a data-loss risk once schema changes are additive/destructive across environments.

## Redis (Phase 2 only)
- `@Cacheable` on read-heavy, rarely-changing queries (e.g. unfiltered task list).
- `@CacheEvict` (or `@CachePut`) on every create/update/delete that could invalidate that cache — evict, don't just set a short TTL and hope.
- Name cache keys explicitly (`@Cacheable(value = "tasks", key = "#filter")`) so eviction rules are easy to reason about.

## Testing
- Unit: JUnit 5 + Mockito, mock the repository, test the service layer's branching logic (not the repository, not Spring wiring).
- Integration (Phase 2+): Testcontainers spinning up real Postgres/Redis when the thing under test is the persistence or caching behavior itself.
- Run `./mvnw test` after every change that touches service/controller logic.

## Do NOT
- Don't add Spring Security, JWT, or multi-tenancy speculatively — only when the roadmap step calls for it.
- Don't reach for a mapping/validation library to save a few lines in Phase 1 — writing it by hand is the point.
- Don't put business logic in the controller "just this once."