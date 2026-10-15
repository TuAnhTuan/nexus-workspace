# Todo App — CLAUDE.md

## Project goal
Build a full-stack Todo List app to learn Java core/Spring Boot while ending up with something usable.
Follow 2 phases:
1. **Phase 1 (learn the core):** Basic CRUD — add/edit/delete/mark task complete. Prioritize understanding Java, Spring Boot, JPA, REST over optimization.
2. **Phase 2 (extend/scale):** priority, deadline, category, search, pagination, Redis cache, auth, etc.

Do not jump to Phase 2 until Phase 1 runs reliably and is well understood.

## Tech stack
- **Backend:** Java 21, Spring Boot 3.x (Spring Web, Spring Data JPA, Spring Validation)
- **Database:** PostgreSQL (docker-compose for local dev)
- **Cache:** Redis (Phase 2 only — cache task lists, sessions, rate limiting...)
- **Frontend:** Vue.js (Vite), fetch/axios calling the REST API
- **Build tool:** Maven (backend), npm/yarn (frontend)

## Directory structure (planned)
```
todo-list/
├── backend/
│   ├── src/main/java/com/todoapp/
│   │   ├── controller/     # REST controllers
│   │   ├── service/        # business logic
│   │   ├── repository/     # Spring Data JPA repositories
│   │   ├── entity/         # JPA entities
│   │   ├── dto/             # request/response DTOs
│   │   ├── exception/      # exception handling
│   │   └── config/         # Redis, CORS, security config
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── api/            # backend calls
│   │   └── App.vue
│   └── package.json
├── docker-compose.yml       # postgres + redis
├── CLAUDE.md
└── .claude/
    ├── skills/todo-app-dev/SKILL.md
    └── agents/{backend-dev.md, frontend-dev.md}
```

## Code conventions
- **Backend:** package by layer (controller/service/repository/entity/dto). Entities are never exposed directly via the API — always go through a DTO. Validate input with `@Valid` + Bean Validation. Handle errors centrally with `@ControllerAdvice`.
- **REST API convention:** `/api/v1/tasks`, use the correct HTTP method (GET/POST/PUT/PATCH/DELETE) and status code (200/201/204/400/404).
- **Frontend:** Vue SFCs with the Composition API (`<script setup>`), keep API calls in the `api/` folder, never call fetch directly inside a component.
- **Naming:** standard Java conventions (PascalCase classes, camelCase methods/fields). Vue components in PascalCase (`.vue` files).
- **Commits:** short, accurately describe the change (no strict convention required, but stay consistent).

## Common commands
```bash
# Backend
cd backend && ./mvnw spring-boot:run
cd backend && ./mvnw test

# Frontend
cd frontend && npm install && npm run dev

# Infra (Postgres + Redis)
docker-compose up -d
```

## Initial data model (Task)
| field | type | notes |
|---|---|---|
| id | Long | PK |
| title | String | required |
| description | String | optional |
| completed | boolean | default false |
| createdAt | Instant | auto |
| updatedAt | Instant | auto |

Phase 2 will add: `priority`, `dueDate`, `category`.

## Working in this repo
- Before coding a new feature, read `.claude/skills/todo-app-dev/SKILL.md` to know which step of the roadmap you're on.
- Backend work should go to the `backend-dev` agent, frontend work to the `frontend-dev` agent (see `.claude/agents/`).
- Always briefly explain why the code is written the way it is (the goal is learning, not just making it work) while the user is in Phase 1.
