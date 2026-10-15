---
name: frontend
description: Vue.js/Vite conventions for the Todo App frontend — component structure, API layer, state, routing, forms, styling, testing, and the growth path from Phase 1 to Phase 2. Use whenever writing or reviewing code under frontend/src, or when unsure how a UI concern should be structured in this project.
---

# Frontend — Vue.js conventions

Reference this skill whenever touching `frontend/`. It captures the *how*; `todo-app-dev` tracks *what backend fields/features currently exist* so the UI doesn't get ahead of the API. Everything here scales from a single-page Phase 1 app to a multi-view Phase 2 app — grow into the sections marked "Phase 2+", don't pre-build them.

## Structure

Phase 1 (current):
```
src/
  api/          all fetch/axios calls, one file per resource (tasks.js)
  components/   reusable presentational + small stateful pieces (.vue)
  pages/        route-level components (views) composing components/ + api/
  App.vue
  main.js
```

Phase 2+, grow one folder at a time, only when the need is real:
```
src/
  api/          HTTP calls per resource + a shared client (axios instance / fetch wrapper)
  assets/       static files (images, global.css)
  components/   dumb/reusable pieces (TaskItem, TaskForm, Pagination, CategoryBadge...)
  composables/  reusable stateful logic (useTasks, usePagination, useDebouncedSearch)
  router/       index.js — route table, lazy-loaded views
  stores/       Pinia stores (introduced only in Phase 2, see State below)
  views/        route-level components (renamed from pages/ once vue-router lands)
  utils/        pure helper functions (formatDate, debounce) — no Vue imports here
  App.vue
  main.js
```
Components never call `fetch`/`axios` directly — always through a function in `api/`. This keeps HTTP concerns (base URL, error shape, headers, auth token) in one place.

## Components
Use `<script setup>` SFCs (Composition API) — not the Options API, not class components.
```vue
<script setup>
import { ref, onMounted } from 'vue'
import { getTasks } from '@/api/tasks'

const tasks = ref([])
const loading = ref(true)
const error = ref(null)

onMounted(async () => {
  try {
    tasks.value = await getTasks()
  } catch (e) {
    error.value = e
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <p v-if="loading">Loading...</p>
  <p v-else-if="error">Error: {{ error.message }}</p>
  <ul v-else>
    <li v-for="task in tasks" :key="task.id">{{ task.title }}</li>
  </ul>
</template>
```
- Props with `defineProps` (destructure with defaults via `withDefaults` when needed), events with `defineEmits`. Type them with JSDoc or plain objects — don't add TypeScript unless asked.
- Keep components small and single-purpose: a list renders items via a child component (`TaskItem`) rather than inlining markup for every row once it grows past a few lines.
- Prefer `computed` over recomputing derived values inside the template or re-deriving them in multiple places.
- Don't reach for global state for data a parent/child relationship already covers — pass props down, emit events up.

## Composables (Phase 2+)
When logic needs to be reused across components (e.g. fetching + pagination state, a debounced search box, toggling a task's completed state with optimistic UI), extract it into `src/composables/useX.js` instead of copy-pasting the same `ref`/`onMounted` block:
```js
// src/composables/useTasks.js
import { ref } from 'vue'
import { getTasks } from '@/api/tasks'

export function useTasks() {
  const tasks = ref([])
  const loading = ref(true)
  const error = ref(null)

  async function load(params) {
    loading.value = true
    error.value = null
    try {
      tasks.value = await getTasks(params)
    } catch (e) {
      error.value = e
    } finally {
      loading.value = false
    }
  }

  return { tasks, loading, error, load }
}
```
A composable is the Vue equivalent of a custom hook: a plain function starting with `use`, returning refs/functions, called from `<script setup>`. Don't introduce one until at least two components actually need the same logic.

## State
- **Phase 1:** `ref`/`reactive` + `computed` for local/page state, lifted via props/emits where a parent needs it. No store.
- **Phase 2+:** introduce Pinia only when a real cross-page need appears. Auth is the planned trigger (see Authentication below); shared task data across unrelated routes (e.g. a nav-bar count fed by the same list a page renders) is another. Don't add Pinia to solve prop-drilling that's only 1-2 levels deep; a composable or well-placed provide/inject usually covers that first.

## API layer pattern
```js
// src/api/tasks.js
const BASE_URL = "/api/v1/tasks";

export async function getTasks() {
  const res = await fetch(BASE_URL);
  if (!res.ok) throw new Error(`Failed to fetch tasks: ${res.status}`);
  return res.json();
}
```
Every function: throws (or returns an error shape) on non-2xx, so the caller can drive a loading/error state instead of failing silently.

**Phase 2+ shared client:** once there's more than one resource and/or an auth token to attach, factor a shared client instead of repeating `BASE_URL`/headers/error-handling per file:
```js
// src/api/client.js
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

export async function apiFetch(path, options = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options.headers },
  })
  if (!res.ok) throw new Error(`Request failed: ${res.status}`)
  if (res.status === 204) return null
  return res.json()
}
```
Then `tasks.js` calls `apiFetch('/tasks')`, `apiFetch('/tasks', { method: 'POST', body: JSON.stringify(payload) })`, etc. Each resource file still owns its own function names (`getTasks`, `createTask`) — the shared client only removes duplication of the fetch/error boilerplate.

## Loading & error states
Every view that fetches data needs at minimum:
```vue
<p v-if="loading">Loading...</p>
<p v-else-if="error">Error: {{ error.message }}</p>
```
Not elaborate — just present. A blank screen on failure is a bug. For actions (create/update/delete/toggle), surface failure near the control that triggered it (inline message or a lightweight toast), not just in the console.

## Forms & validation
- A plain `reactive`/`ref`-backed form object is enough; bind with `v-model`.
- Validate on submit at minimum (e.g. title not blank) and mirror whatever Bean Validation enforces server-side — the frontend check is for UX (fail fast, no round trip), the backend is still the source of truth.
- Show the field-level error returned by the backend's 400 response rather than inventing a different message.
- Don't add a form library (VeeValidate, FormKit) until forms have enough fields/rules that hand-rolling it is genuinely painful — not for a title+description+priority form.

## Routing (Phase 2+)
Add `vue-router` once there's a second real view (e.g. a task detail/edit page, or a dedicated search/filter page) — not before. Routes live in `src/router/index.js`; lazy-load route components so the initial bundle stays small:
```js
const routes = [
  { path: '/', component: () => import('@/views/TaskListView.vue') },
  { path: '/tasks/:id', component: () => import('@/views/TaskDetailView.vue') },
]
```

## Authentication (Phase 2 — confirmed, build only once backend auth endpoints exist)
Auth is a committed part of the roadmap (Spring Security + JWT on the backend). Design decisions are fixed here so frontend and backend agree before either is built; if the backend ends up with a different contract, update this section first, then the code.

### API contract the frontend expects
| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/api/v1/auth/register` | `{ email, password }` | `201` + `{ accessToken, user }` |
| POST | `/api/v1/auth/login` | `{ email, password }` | `200` + `{ accessToken, user }`, sets refresh cookie |
| POST | `/api/v1/auth/refresh` | — (cookie) | `200` + `{ accessToken, user }` |
| POST | `/api/v1/auth/logout` | — (cookie) | `204`, clears refresh cookie |

`user` is a small DTO (`id`, `email`, maybe `displayName`) — never the full entity.

### Token storage — why this split
- **Access token** (short-lived JWT, ~15 min): kept **in memory only** (Pinia state). Not in `localStorage`/`sessionStorage`: any XSS bug could read those, and a stolen token works until it expires.
- **Refresh token** (long-lived): an **`httpOnly`, `Secure`, `SameSite=Strict` cookie set by the backend**. JavaScript can't read it at all, so XSS can't steal it. The browser sends it automatically to `/auth/refresh`.
- On page reload, memory is wiped, so the app calls `/auth/refresh` once at startup to get a new access token silently. That's how the user stays logged in without the token sitting in storage.
- Simpler alternative (access token in `localStorage`, no refresh) is acceptable *only* if explicitly chosen for learning speed; document the choice here if so. Don't mix the two approaches.

### Dev setup: Vite proxy instead of cross-origin calls
Proxy `/api` to the backend in `vite.config.js` so the browser sees one origin. Cookies (`SameSite=Strict`) then just work, and CORS mostly stops being a dev concern:
```js
server: { proxy: { '/api': 'http://localhost:8080' } }
```

### Auth store
```js
// src/stores/auth.js
import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({ accessToken: null, user: null, initialized: false }),
  getters: {
    isAuthenticated: (s) => !!s.accessToken,
  },
  actions: {
    async login(credentials) {
      const { accessToken, user } = await authApi.login(credentials)
      this.accessToken = accessToken
      this.user = user
    },
    async restoreSession() {
      try {
        const { accessToken, user } = await authApi.refresh()
        this.accessToken = accessToken
        this.user = user
      } catch {
        this.$reset()
      } finally {
        this.initialized = true
      }
    },
    async logout() {
      try { await authApi.logout() } finally { this.$reset() }
    },
  },
})
```

### Shared client: attach token, refresh once on 401
```js
// src/api/client.js (auth-aware version)
import { useAuthStore } from '@/stores/auth'
import * as authApi from '@/api/auth'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
let refreshing = null
let onSessionExpired = () => {}
export function setSessionExpiredHandler(fn) { onSessionExpired = fn }

export async function apiFetch(path, options = {}, retried = false) {
  const auth = useAuthStore()
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(auth.accessToken && { Authorization: `Bearer ${auth.accessToken}` }),
      ...options.headers,
    },
  })

  if (res.status === 401 && !retried) {
    refreshing ??= authApi.refresh().finally(() => { refreshing = null })
    try {
      const { accessToken, user } = await refreshing
      auth.accessToken = accessToken
      auth.user = user
      return apiFetch(path, options, true)
    } catch {
      auth.$reset()
      onSessionExpired()
      throw new Error('Session expired')
    }
  }
  if (!res.ok) throw new Error(`Request failed: ${res.status}`)
  return res.status === 204 ? null : res.json()
}
```
- `refreshing` is shared so five parallel requests that all get 401 trigger **one** refresh call, not five.
- `api/auth.js` uses plain `fetch` with `credentials: 'include'`, **not** `apiFetch`. Otherwise a 401 from `/auth/refresh` would try to refresh itself in a loop.
- The client doesn't import the router (that would create a circular import). `main.js` registers `setSessionExpiredHandler(() => router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } }))`.

### Router guards
```js
// src/router/index.js
const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { guestOnly: true } },
  { path: '/register', name: 'register', component: () => import('@/views/RegisterView.vue'), meta: { guestOnly: true } },
  { path: '/', name: 'tasks', component: () => import('@/views/TaskListView.vue'), meta: { requiresAuth: true } },
]

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.initialized) await auth.restoreSession()
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && auth.isAuthenticated) return { name: 'tasks' }
})
```
After a successful login, go to `route.query.redirect` if present (only if it starts with `/`, to avoid open redirects), else `/`.

### UI rules
- `LoginView` / `RegisterView` live in `views/`; the shared form pieces go in `components/` (`AuthForm.vue`).
- Show the backend's message for `401` (wrong credentials) and `409` (email already registered). Don't reveal which of email/password was wrong; the backend shouldn't either.
- Clear the password field after any submit attempt. Never log credentials or tokens, even in `console.log` while debugging.
- Disable the submit button while the request is in flight (prevents double submission).
- On logout, also reset any task-related state/stores so the next user on the same browser never sees the previous user's tasks.
- Hiding a button with `v-if="auth.isAuthenticated"` is **UX only**. The backend must still enforce every permission; the frontend never decides access on its own (and never trusts a decoded JWT for that).

## Styling
Plain CSS or scoped `<style scoped>` blocks per component by default. Don't pull in Vuetify/PrimeVue/Tailwind unless asked — Phase 1 is about function, not polish. If the app grows enough repeated visual patterns (buttons, badges, form fields) that copy-pasted CSS becomes its own maintenance problem, extract shared rules into `src/assets/base.css` before reaching for a UI kit.

## Environment config
Use Vite env vars (`.env`, `.env.development`, `.env.production`), prefixed `VITE_` (e.g. `VITE_API_BASE_URL`), read via `import.meta.env.VITE_API_BASE_URL`. Never hardcode a deployed backend URL in source; never commit real secrets — Phase 1 doesn't need any (no auth tokens yet).

## Performance notes (apply as they become relevant, not preemptively)
- Always key `v-for` with a stable id (`:key="task.id"`), never the array index.
- Prefer `computed` for derived/filtered lists over recalculating in the template or in a `watch` side effect.
- Once lists get large (Phase 2 pagination), rely on the backend's `Pageable` response rather than fetching everything and paginating client-side.
- Lazy-load routes (see Routing) once there is more than one view.

## Accessibility baseline
- Every form input has an associated `<label>`.
- Buttons are `<button>`, not clickable `<div>`s.
- Interactive elements are reachable and operable via keyboard (native elements give this for free — don't replace them with styled `<div>`/`<span>` unless there's a real reason).

## Testing
See the `testing` skill for the full policy. Summary for frontend: hand-test the UI against the running backend for Phase 1 features; once component logic gets non-trivial (client-side filtering/sorting, a composable with branching logic), add Vitest (+ Vue Testing Library for anything rendering) for that specific logic — not for trivial presentational components.

## Linting & formatting
Use ESLint with the official Vue plugin (`eslint-plugin-vue`) and Prettier, run via `npm run lint` / the editor. Fix lint errors before considering a change done; don't disable a rule to silence it without a reason.

## Naming
- Components: PascalCase, one component per `.vue` file, filename matches component name (`TaskList.vue` → `<TaskList>`).
- Composables: camelCase starting with `use` (`useTasks.js` → `useTasks()`).
- Pinia stores: camelCase starting with `use`, ending `Store` (`useAuthStore`).
- Functions/variables: camelCase.
- API functions named after the action + resource: `getTasks`, `createTask`, `toggleTaskComplete`.

## Growth path — mapping Phase 2 backend features to frontend work
Check `.claude/skills/todo-app-dev/SKILL.md` for which step is actually active before building any of this; the list below is *where it lands*, not a queue to build ahead of the API.
- **priority / dueDate / category fields**: add to `TaskForm` + `TaskItem` display (e.g. a `CategoryBadge`, a due-date formatter in `utils/`). No new architecture needed.
- **search & filter**: a filter bar component holding its own state (or a `useDebouncedSearch` composable), passed as query params to `getTasks(params)`.
- **pagination**: switch the list view to read `page`/`totalPages` from the backend's `Page<TaskResponse>` response; add a `Pagination` component. This is usually the point where client-only state in a single page component starts to feel cramped — if a count/summary needs to live outside that page too, that's a legitimate Pinia trigger.
- **Redis cache**: backend-only; no frontend change required.
- **auth** (confirmed): follow the Authentication section above. It brings in Pinia, `vue-router` (if not already added), the auth-aware shared client, and login/register views. Build in this order: Vite proxy → `api/auth.js` → auth store → client token/refresh → router guards → views.
- **advanced testing / observability**: backend-only; frontend testing stays scoped per the `testing` skill.

## Staying in sync with the backend
Before adding a field to a form or list (priority, dueDate, category), confirm the backend DTO already exposes it — check `.claude/skills/todo-app-dev/SKILL.md` for which Phase 2 step is active, or ask. Don't build UI for fields the API doesn't return yet.

## Do NOT
- Don't call `fetch`/`axios` inside a component's `<script setup>` body directly for anything beyond invoking an `api/` function.
- Don't add authentication UI or a Pinia auth store before the backend has auth endpoints.
- Don't add a build-time dependency (Pinia, vue-router, a UI kit, a form library) without it being asked for or clearly required by the current step — see Growth path above for when each one actually earns its place.
- Don't mix Options API and Composition API in the same component.
- Don't fetch an entire resource client-side and paginate/filter it in JS once the backend offers real pagination/search — that defeats the point of the Phase 2 work.
