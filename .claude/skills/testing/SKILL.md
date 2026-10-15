---
name: testing
description: Testing conventions for the Todo App project — JUnit/Mockito for backend unit tests, Testcontainers for integration tests, what (and when) to test on the frontend. Use whenever writing new backend/frontend code that needs a test, or asked to add test coverage.
---

# Testing — conventions

Testing exists to catch regressions and to double-check understanding while learning — not to hit a coverage number. Prefer a few meaningful tests over exhaustive ones in Phase 1.

## Backend — unit tests (from Phase 1)
- Framework: JUnit 5 + Mockito, already on the Spring Boot Initializr `spring-boot-starter-test` dependency.
- Test the **service layer** — mock `TaskRepository`, assert on behavior (what gets saved, what gets thrown, what gets returned), not on internal implementation details.
- Minimum coverage per feature: one happy-path test, one not-found/error-path test. E.g. for `TaskService.getById`: found → returns mapped DTO; not found → throws `TaskNotFoundException`.
- Naming: `methodName_condition_expectedResult` (e.g. `getById_taskNotFound_throwsException`) or a descriptive `@DisplayName` — either is fine, stay consistent within a test class.
- Run `./mvnw test` after writing service/controller logic, before considering the step done.

## Backend — integration tests (Phase 2+)
- Use Testcontainers when the thing under test is genuinely the persistence or cache behavior itself (a real repository query, a real `@Cacheable` eviction) — not as a default replacement for unit tests.
- Spin up Postgres (and Redis once caching exists) as containers in the test, not against the dev docker-compose instance — tests must be runnable in isolation.
- Keep these separate from unit tests (e.g. `@Tag("integration")` or a separate source set) since they're slower and need Docker.

## Frontend
- Phase 1: manual verification (run `npm run dev`, click through add/edit/delete/toggle) is acceptable — don't introduce a test framework (Vitest/Vue Testing Library) before there's enough component logic to justify it.
- If/when component logic gets non-trivial (e.g. filtering/sorting done client-side in Phase 2), add Vitest + Vue Testing Library for that specific logic, not for trivial presentational components.
- Test behavior a user would notice (list renders fetched tasks, form submit calls the right API function) — not implementation details like internal state variable names.

## What NOT to test
- Don't test Spring Data JPA's generated query methods themselves (`findByCompleted`) — that's testing the framework, not your code.
- Don't test getters/setters or trivial DTO mapping with no branching.
- Don't write a UI snapshot test suite for a project this size — snapshots rot and get blindly updated, defeating the purpose.

## Definition of done (testing angle)
A feature isn't done until: relevant service-layer tests pass, and (if the change is API-facing) it's been exercised at least once via curl/Postman or the running frontend — automated tests don't replace that manual check in Phase 1.