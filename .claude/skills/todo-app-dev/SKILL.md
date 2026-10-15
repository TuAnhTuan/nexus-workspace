---
name: todo-app-dev
description: Roadmap for building the Todo List app (Spring Boot + Vue.js + PostgreSQL + Redis), from basic CRUD (learning Java core) to extend/scale. Use this skill whenever working in the todo-list repo, adding a feature, or unsure what step comes next.
---

# Todo App Dev — build roadmap

This skill is the "roadmap" for the todo app project. Read it carefully before starting/continuing a work session, and always confirm which step the user is on before writing code.

## General principles
1. Don't build Phase 2 features while Phase 1 isn't finished and well understood.
2. Each step: write the minimum code needed to run, briefly explain the relevant Java/Spring concept, then move to the next step.
3. Prefer building/testing after each small step rather than batching many changes before testing.
4. If the user asks "why", prioritize answering to help them learn rather than just handing over code.

## Phase 1 — Basic CRUD (learning Java core / Spring Boot)

Suggested order:
1. **Project setup**: Spring Initializr (Web, JPA, PostgreSQL driver, Validation, Lombok optional), create `docker-compose.yml` to run Postgres.
2. **`Task` entity**: id, title, description, completed, createdAt, updatedAt. Explain JPA annotations (`@Entity`, `@Id`, `@GeneratedValue`).
3. **Repository**: `TaskRepository extends JpaRepository<Task, Long>`. Explain how Spring Data JPA generates queries automatically.
4. **DTO**: `TaskRequest` (input), `TaskResponse` (output). Explain why entities shouldn't be exposed directly.
5. **Service**: `TaskService` holding business logic (create/update/delete/list/toggle complete). Explain separating logic from the controller.
6. **Controller**: REST endpoints:
   - `GET /api/v1/tasks`
   - `GET /api/v1/tasks/{id}`
   - `POST /api/v1/tasks`
   - `PUT /api/v1/tasks/{id}`
   - `PATCH /api/v1/tasks/{id}/complete`
   - `DELETE /api/v1/tasks/{id}`
7. **Exception handling**: `@ControllerAdvice` for 404 (task not found), 400 (validation error).
8. **Unit tests**: test `TaskService` with JUnit + Mockito, at minimum a create flow and a not-found flow.
9. **Basic frontend**: Vue list showing tasks, add-task form, delete/toggle-complete buttons, calling the API via `api/tasks.js`.

Phase 1 completion checklist:
- [ ] Create/edit/delete/toggle task via API works correctly
- [ ] Validation in place (title not blank)
- [ ] Clear 404/400 error handling
- [ ] At least a few unit tests passing
- [ ] Frontend displays and interacts with the real API

## Phase 2 — Extend & scale

Only start after Phase 1 runs reliably. Suggested order, one at a time:

1. **Extended fields**: `priority` (enum LOW/MEDIUM/HIGH), `dueDate` (LocalDate), `category` (String or its own entity). Requires migrations (Flyway/Liquibase recommended over `ddl-auto: update`).
2. **Search & filter**: query by `completed`, `category`, `priority`, `dueDate` range. Use Spring Data JPA Specifications or query methods.
3. **Pagination & sorting**: `Pageable`, return `Page<TaskResponse>`.
4. **Redis cache**: cache task lists (e.g. unfiltered `GET /tasks`) via `@Cacheable`/`@CacheEvict`. Explain cache invalidation on create/update/delete.
5. **Auth (confirmed — will be built)**: Spring Security + JWT for multi-user support, each user sees only their own tasks. Short-lived access token in the response body + refresh token in an `httpOnly` cookie; the endpoint contract (`/api/v1/auth/register|login|refresh|logout`) and frontend flow are defined in the `frontend` skill's Authentication section. Backend and frontend must match it.
6. **Advanced testing**: integration tests with Testcontainers (real Postgres + Redis in containers).
7. **Observability (optional)**: Spring Actuator, structured logging.

## Which agent to use
- Pure backend work (entity, repository, service, controller, Redis config, Java tests) → `backend-dev` agent.
- Pure frontend work (Vue components, API calls, UI/UX) → `frontend-dev` agent.
- Work spanning both (e.g. a new field needing API + form changes) → do backend first, then frontend, possibly using both agents in sequence.

## Definition of "done"
A feature is done when: the code builds, basic tests pass, the API works as expected when tested via curl/Postman, and (if applicable) the frontend displays it correctly.
