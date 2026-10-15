---
name: docs
description: Conventions for writing/updating documentation in the Todo App project — root README, API reference, and inline doc comments. Use whenever asked to document an endpoint, write a README, or explain project setup for a new contributor.
---

# Docs — writing conventions

This project has two audiences for docs: the user themself (learning, so docs should explain *why*) and a hypothetical future contributor (so docs should also be self-sufficient without conversation history).

## Root README.md
Keep it to what's needed to get the app running, not a feature list that will drift:
1. One-line project description + link to `CLAUDE.md` for the full roadmap/conventions.
2. Prerequisites (Java 21, Node, Docker).
3. Setup steps: `docker-compose up -d`, backend run command, frontend run command — copy these from the "Common commands" section of `CLAUDE.md` rather than re-deriving them, so the two files don't drift.
4. Where to find things: point at `backend/`, `frontend/`, `docker-compose.yml`.

Don't restate the full data model or API list in the README if it's already accurate in `CLAUDE.md` or generated API docs — link instead of duplicating, since duplicated docs are the ones that go stale.

## API documentation
Prefer documenting endpoints where they're defined:
- A short Javadoc-style comment above each controller method stating method + path + one-line purpose is usually enough for Phase 1.
- If the API surface grows past ~10 endpoints (Phase 2), consider generating an OpenAPI spec via `springdoc-openapi` rather than hand-maintaining a markdown table — hand-written API tables rot the moment someone changes a field without remembering to update the doc.

## Inline comments
Follow the same policy as code: no comment unless it explains a non-obvious *why* (a workaround, a constraint, a gotcha). Don't write comments that restate what the code already says — this project's whole point is readable code, and restating it in prose undermines that.

## When documenting a new feature
After a backend or frontend change lands, check whether it invalidates something already documented in the README or `CLAUDE.md` (e.g. a new field, a new common command, a changed directory). Update the stale doc in the same pass — don't leave it for later.

## Do NOT
- Don't create a docs/ directory with speculative pages (architecture.md, contributing.md, etc.) unless asked — for a learning project with one contributor, that's overhead with no reader.
- Don't auto-generate long docstrings/comment blocks; this project's convention (see `CLAUDE.md`) is minimal comments.