---
name: debug
description: Debugging approach for the Todo App project — reading Spring Boot stack traces, diagnosing 4xx/5xx API errors, tracing frontend fetch failures, common Postgres/Redis connection issues. Use whenever something errors, a request returns an unexpected status, or behavior doesn't match expectations.
---

# Debug — triage approach

Goal in this project isn't just "make the error go away" — it's understanding *why* it happened, since Phase 1 is explicitly about learning. Explain the root cause in plain terms before fixing it.

## General approach
1. **Reproduce first.** Get the exact request/action that fails (curl command, UI step, or failing test) before changing code. Don't guess-fix.
2. **Read the whole stack trace**, bottom-up isn't always right — for Spring, the *caused by* chain at the bottom is usually the real error; the top frames are often just the dispatcher/filter chain.
3. **Isolate the layer.** Is it the frontend (network tab shows the wrong request), the controller (wrong route/method), the service (wrong logic), the repository (wrong query), or the DB/cache (data not what's expected)? Narrow before reading code.
4. **Fix the root cause**, not the symptom — e.g. a `NullPointerException` on `task.getCategory()` usually means a missing DTO mapping or an un-migrated column, not a spot `!= null` check.

## Backend-specific
- **404 vs 400 vs 500 confusion**: 404 = `TaskNotFoundException` should be thrown by the service, not silently returning null. 400 = validation should be caught by `@Valid` + the `@RestControllerAdvice`, not a manual `if` in the controller. If you're seeing a raw 500 for something that should be a 4xx, the exception isn't being caught by the advice — check the exception type matches what the handler declares.
- **`LazyInitializationException`**: usually means an entity's association is accessed after the transaction/session closed (e.g. in a DTO mapper called from the controller). Fix by mapping to DTO inside the `@Transactional` service method, not after.
- **Connection refused to Postgres/Redis**: check `docker ps` — compose not up, or the app started before the container was ready (Phase 1 doesn't need a wait-for-it script; just restart the app once compose is healthy).
- **Schema mismatch after adding a field**: with `ddl-auto=update` this is usually silent-partial; if a field isn't showing up, check the actual table with `\d tasks` in `psql` rather than assuming the entity change took effect.

## Frontend-specific
- **Fetch fails silently**: check the Network tab first — status code and response body tell you whether it's a CORS issue (check `@CrossOrigin`/CORS config backend-side), a 4xx (bad request shape), or the backend not running at all.
- **CORS errors**: only fixable backend-side (`config/CorsConfig.java` or `@CrossOrigin` on the controller) — don't try to work around it with frontend proxy hacks in Phase 1 unless Vite's dev proxy is already the established pattern.
- **Stale UI after mutation**: check whether the component re-fetches or updates local state after create/update/delete — a common Phase 1 bug is refetching only on mount.

## When stuck
If the fix isn't obvious after checking the layer boundaries above, add a temporary log/print at the layer boundary (controller entry, service entry, repository call) to see where behavior diverges from expectation — then remove the temporary logging once resolved, don't leave debug prints in.

## Do NOT
- Don't wrap exceptions in generic try/catch that swallows the error just to stop it from surfacing.
- Don't add defensive null-checks as the fix when the real issue is a missing DTO mapping or an unhandled exception type.