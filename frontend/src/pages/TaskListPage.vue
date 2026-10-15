<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import {
  createTask,
  deleteTask,
  getRecommendations,
  getTasks,
  rescheduleTask,
  toggleTaskComplete,
  updateTask,
  updateTaskProgress,
} from '@/api/tasks'
import AppIcon from '@/components/AppIcon.vue'
import AppSidebar from '@/components/AppSidebar.vue'
import RecommendationPanel from '@/components/RecommendationPanel.vue'
import TaskBoard from '@/components/TaskBoard.vue'
import TaskDetail from '@/components/TaskDetail.vue'
import TaskForm from '@/components/TaskForm.vue'
import { useSettings } from '@/composables/useSettings'
import { today } from '@/utils/formatDate'

/**
 * The page owns the tasks array, UI state (filter, search, selection) and every API call.
 * Child components only display and emit events.
 * After each successful request we patch the local array from the response instead of
 * re-fetching the whole list: one request per action, and the UI shows exactly what the DB saved.
 */
const { t } = useSettings()

const tasks = ref([])
const loading = ref(true)
const loadError = ref(null)

const filter = ref('all')
const search = ref('')
const selectedId = ref(null)
const sidebarOpen = ref(false) // only matters on phones, where the sidebar is a drawer

// One dialog for both create and edit: editingTask === null → create mode.
const editingTask = ref(null)
const saving = ref(false)
const formError = ref(null)
const taskForm = ref(null) // template ref → TaskForm's exposed reset()
const formDialog = ref(null) // template ref → native <dialog>

// Per-task state keyed by id, so one task's request doesn't block or show errors on the others.
const busy = reactive({})
const itemErrors = reactive({})

/** Network errors carry a `code` so they can be shown in the current language. */
function errorText(e) {
  if (!e) return ''
  return e.code === 'offline' ? t('status.offline') : e.message
}

// "Today" = planned for today, plus anything overdue that isn't done (it still needs doing today).
function isForToday(task) {
  const day = today()
  return task.dueDate === day || (!task.completed && !!task.dueDate && task.dueDate < day)
}

const counts = computed(() => {
  const done = tasks.value.filter((x) => x.completed).length
  return {
    all: tasks.value.length,
    today: tasks.value.filter(isForToday).length,
    todo: tasks.value.length - done,
    completed: done,
  }
})

// Which board columns each sidebar filter shows.
const FILTER_COLUMNS = {
  all: ['todo', 'inProgress', 'completed'],
  today: ['todo', 'inProgress', 'completed'],
  todo: ['todo', 'inProgress'],
  completed: ['completed'],
}

// Client-side filtering/search is fine while the backend has no search endpoint.
// Phase 2 step 2 (search & filter) moves this to query params on GET /tasks.
const visibleTasks = computed(() => {
  const q = search.value.trim().toLowerCase()
  return tasks.value.filter(
    (x) =>
      (filter.value !== 'today' || isForToday(x)) &&
      (!q || x.title.toLowerCase().includes(q) || x.description?.toLowerCase().includes(q)),
  )
})

// Average % over the tasks currently shown: partial work counts, not just finished tasks.
const avgProgress = computed(() => {
  const list = visibleTasks.value
  return list.length ? Math.round(list.reduce((sum, x) => sum + x.progress, 0) / list.length) : 0
})
const visibleDone = computed(() => visibleTasks.value.filter((x) => x.completed).length)

// ---- Recommendations ----
const recommendations = ref([])
const dismissed = ref(new Set()) // "taskId-TYPE" keys hidden until the page reloads
const visibleRecommendations = computed(() =>
  recommendations.value.filter((r) => !dismissed.value.has(`${r.taskId}-${r.type}`)),
)

/** Suggestions depend on every task's state, so re-fetch after any change. Failures are non-critical. */
async function loadRecommendations() {
  try {
    recommendations.value = await getRecommendations()
  } catch {
    recommendations.value = []
  }
}

function dismissRecommendation(key) {
  dismissed.value = new Set(dismissed.value).add(key)
}
const selectedTask = computed(() => tasks.value.find((x) => x.id === selectedId.value) ?? null)

async function load() {
  loading.value = true
  loadError.value = null
  try {
    tasks.value = await getTasks()
    loadRecommendations()
  } catch (e) {
    loadError.value = e
  } finally {
    loading.value = false
  }
}

function openNewTask() {
  editingTask.value = null
  formDialog.value.showModal()
}
function openEditTask(task) {
  editingTask.value = task
  formDialog.value.showModal()
}
function closeTaskDialog() {
  formDialog.value.close() // triggers @close → onDialogClosed
}
/** Runs however the dialog closes (Cancel, Esc, after saving), so cleanup lives in one place. */
function onDialogClosed() {
  taskForm.value.reset()
  formError.value = null
  editingTask.value = null
}

async function onSubmitTask(payload) {
  saving.value = true
  formError.value = null
  try {
    if (editingTask.value) {
      // PUT replaces the whole task and missing progress means 0 on the backend,
      // so we must send the current value or editing would reset the progress.
      const task = editingTask.value
      replaceTask(await updateTask(task.id, { ...payload, progress: task.progress }))
    } else {
      tasks.value.push(await createTask({ ...payload, progress: 0 }))
    }
    closeTaskDialog()
    loadRecommendations()
  } catch (e) {
    formError.value = e // stays open, showing the backend's message
  } finally {
    saving.value = false
  }
}

/** Shared wrapper for task actions: marks the task busy, clears/sets its error. */
async function runForTask(task, action) {
  busy[task.id] = true
  delete itemErrors[task.id]
  try {
    await action()
    loadRecommendations()
  } catch (e) {
    itemErrors[task.id] = e
  } finally {
    delete busy[task.id]
  }
}

function replaceTask(updated) {
  const i = tasks.value.findIndex((x) => x.id === updated.id)
  if (i !== -1) tasks.value[i] = updated
}

function onToggle(task) {
  return runForTask(task, async () => replaceTask(await toggleTaskComplete(task.id)))
}

function onProgress(task, progress) {
  return runForTask(task, async () => replaceTask(await updateTaskProgress(task.id, progress)))
}

function onReschedule(task, dueDate) {
  return runForTask(task, async () => replaceTask(await rescheduleTask(task.id, dueDate)))
}

function rescheduleById(taskId, dueDate) {
  const task = tasks.value.find((x) => x.id === taskId)
  if (task) return onReschedule(task, dueDate)
}

/** "Apply all": run the moves in parallel; each task tracks its own busy/error state. */
function applyAllMoves(moves) {
  return Promise.all(moves.map((m) => rescheduleById(m.taskId, m.date)))
}

function onDelete(task) {
  return runForTask(task, async () => {
    await deleteTask(task.id)
    tasks.value = tasks.value.filter((x) => x.id !== task.id)
    if (selectedId.value === task.id) selectedId.value = null
  })
}

function selectFilter(value) {
  filter.value = value
  sidebarOpen.value = false
}

function closeOverlays() {
  sidebarOpen.value = false
  selectedId.value = null
}
function onKeydown(e) {
  // While the dialog is open, Esc belongs to it (the browser closes it natively);
  // don't also deselect the task behind it.
  if (e.key === 'Escape' && !formDialog.value?.open) closeOverlays()
}

// Time-based rules (e.g. "after 17:00 → carry over") change while the page stays open,
// so refresh suggestions every 5 minutes.
let recTimer
onMounted(() => {
  load()
  window.addEventListener('keydown', onKeydown)
  recTimer = setInterval(loadRecommendations, 5 * 60 * 1000)
})
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  clearInterval(recTimer)
})
</script>

<template>
  <div class="shell">
    <div class="app" :class="{ 'sidebar-open': sidebarOpen, 'detail-open': selectedTask }">
      <aside class="sidebar-col">
        <AppSidebar :model-value="filter" :counts="counts" @update:model-value="selectFilter" />
      </aside>

      <div class="backdrop" @click="closeOverlays" />

      <main class="main">
        <!-- Phone-only top bar (hidden on wider screens via CSS) -->
        <div class="topbar">
          <button type="button" class="icon-btn" :aria-label="t('menu')" @click="sidebarOpen = true">
            <AppIcon name="menu" :size="22" />
          </button>
          <span class="topbar-title">{{ t('appName') }}</span>
          <button type="button" class="icon-btn add-icon" :aria-label="t('page.newTask')" @click="openNewTask">
            <AppIcon name="plus" :size="22" />
          </button>
        </div>

        <div class="search">
          <AppIcon name="search" :size="17" />
          <label for="task-search" class="sr-only">{{ t('page.search') }}</label>
          <input id="task-search" v-model="search" type="search" :placeholder="t('page.search')" />
        </div>

        <header class="page-header">
          <div>
            <h1>{{ filter === 'today' ? t('page.titleToday') : t('page.title') }}</h1>
            <p class="subtitle">{{ t('page.subtitle') }}</p>
          </div>
          <button type="button" class="btn new-btn" @click="openNewTask">
            <AppIcon name="plus" :size="16" />{{ t('page.newTask') }}
          </button>
        </header>

        <div v-if="!loading && !loadError && visibleTasks.length" class="progress">
          <div class="progress-text">
            <span>{{ t('page.avgProgress', { pct: avgProgress }) }}</span>
            <span>{{ t('page.doneCount', { done: visibleDone, total: visibleTasks.length }) }}</span>
          </div>
          <div class="progress-track"><div class="progress-fill" :style="{ width: `${avgProgress}%` }" /></div>
        </div>

        <RecommendationPanel
          v-if="!loading && !loadError"
          :recommendations="visibleRecommendations"
          :tasks="tasks"
          :busy="busy"
          @reschedule="rescheduleById"
          @open="selectedId = $event"
          @dismiss="dismissRecommendation"
          @apply-all="applyAllMoves"
        />

        <p v-if="loading" class="status">{{ t('status.loading') }}</p>
        <div v-else-if="loadError" class="status error">
          <p>{{ t('status.error', { message: errorText(loadError) }) }}</p>
          <button type="button" class="btn" @click="load">{{ t('status.retry') }}</button>
        </div>
        <template v-else>
          <p v-if="search && !visibleTasks.length" class="status">{{ t('board.noResults', { q: search }) }}</p>
          <TaskBoard
            v-else
            :tasks="visibleTasks"
            :columns="FILTER_COLUMNS[filter]"
            :selected-id="selectedId"
            :busy="busy"
            @select="selectedId = $event.id"
            @toggle="onToggle"
          />
        </template>
      </main>

      <aside class="detail-col">
        <TaskDetail
          :task="selectedTask"
          :busy="!!(selectedTask && busy[selectedTask.id])"
          :error-text="selectedTask ? errorText(itemErrors[selectedTask.id]) : ''"
          @toggle="onToggle(selectedTask)"
          @edit="openEditTask(selectedTask)"
          @delete="onDelete(selectedTask)"
          @progress="onProgress(selectedTask, $event)"
          @reschedule="onReschedule(selectedTask, $event)"
          @close="selectedId = null"
        />
      </aside>
    </div>

    <!-- Native <dialog>: showModal() gives focus trapping, Esc-to-close and a backdrop for free. -->
    <dialog
      ref="formDialog"
      class="task-dialog"
      :aria-label="editingTask ? t('page.editTask') : t('page.newTask')"
      @close="onDialogClosed"
    >
      <h2>{{ editingTask ? t('page.editTask') : t('page.newTask') }}</h2>
      <!-- :key remounts the form when switching task/mode, so it starts from that task's values -->
      <TaskForm
        ref="taskForm"
        :key="editingTask?.id ?? 'new'"
        autofocus
        :initial-task="editingTask"
        :submitting="saving"
        :errors="formError?.details?.length ? formError.details : formError ? [errorText(formError)] : []"
        @submit="onSubmitTask"
        @cancel="closeTaskDialog"
      />
    </dialog>
  </div>
</template>

<style scoped>
/* ---------- Desktop (≥1200px): 3 columns inside a floating rounded "app window" ---------- */
.shell {
  min-height: 100dvh;
  padding: 24px;
}
.app {
  position: relative;
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr) 340px;
  height: calc(100dvh - 48px);
  max-width: 1440px;
  margin: 0 auto;
  border-radius: 24px;
  overflow: hidden;
  background: var(--app-bg);
  box-shadow: 0 30px 80px rgb(60 40 20 / 0.12);
}
.sidebar-col {
  background: var(--surface);
  border-right: 1px solid var(--border);
  min-height: 0;
}
.detail-col {
  background: var(--surface);
  border-left: 1px solid var(--border);
  min-height: 0;
}
.main {
  min-width: 0;
  overflow-y: auto;
  padding: 1.75rem 2rem 2.5rem;
}
.backdrop {
  display: none;
}
.topbar {
  display: none;
}

.search {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0 0.9rem;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--muted);
}
.search:focus-within {
  border-color: var(--primary);
}
.search input {
  flex: 1;
  min-width: 0;
  padding: 0.65rem 0;
  border: 0;
  background: transparent;
  color: var(--text);
  font-size: 16px;
  outline: none;
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
  margin: 1.75rem 0 1.25rem;
}
h1 {
  margin: 0;
  font-size: 1.75rem;
  font-weight: 700;
  letter-spacing: -0.01em;
}
.subtitle {
  margin: 0.25rem 0 0;
  color: var(--muted);
  font-size: 0.9rem;
}

.progress {
  margin-bottom: 1.5rem;
}
.progress-text {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.4rem;
  color: var(--muted);
  font-size: 0.8rem;
  font-weight: 600;
}
.progress-track {
  height: 8px;
  border-radius: 999px;
  background: var(--surface-2);
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--primary), var(--done));
  transition: width 0.3s ease;
}

.status {
  padding: 2rem 0;
  text-align: center;
  color: var(--muted);
}
.status.error {
  color: var(--danger);
}

.task-dialog {
  width: min(480px, calc(100% - 2rem));
  padding: 1.5rem;
  border: 0;
  border-radius: 20px;
  background: var(--surface);
  color: var(--text);
  box-shadow: var(--shadow);
}
.task-dialog::backdrop {
  background: rgb(15 17 30 / 0.35);
  backdrop-filter: blur(2px);
}
.task-dialog h2 {
  margin: 0 0 1rem;
  font-size: 1.2rem;
}

/* ---------- Tablet / small laptop (<1200px): detail panel becomes a slide-in drawer ---------- */
@media (max-width: 1199px) {
  .app {
    grid-template-columns: 230px minmax(0, 1fr);
  }
  .detail-col {
    position: absolute;
    z-index: 20;
    top: 0;
    right: 0;
    bottom: 0;
    width: min(380px, 100%);
    box-shadow: var(--shadow);
    transform: translateX(100%);
    visibility: hidden;
    transition:
      transform 0.25s ease,
      visibility 0.25s;
  }
  .detail-open .detail-col {
    transform: none;
    visibility: visible;
  }
  .detail-open .backdrop {
    display: block;
  }
  .backdrop {
    position: absolute;
    inset: 0;
    z-index: 10;
    background: rgb(15 17 30 / 0.3);
  }
}

/* ---------- Phone (<860px): full-screen app, sidebar drawer, detail as bottom sheet ---------- */
@media (max-width: 859px) {
  .shell {
    padding: 0;
  }
  .app {
    display: block;
    height: auto;
    min-height: 100dvh;
    border-radius: 0;
    box-shadow: none;
    overflow: visible;
  }
  .backdrop {
    position: fixed;
  }
  .sidebar-open .backdrop {
    display: block;
  }
  .sidebar-col {
    position: fixed;
    z-index: 20;
    top: 0;
    bottom: 0;
    left: 0;
    width: min(290px, 85vw);
    transform: translateX(-100%);
    visibility: hidden;
    transition:
      transform 0.25s ease,
      visibility 0.25s;
  }
  .sidebar-open .sidebar-col {
    transform: none;
    visibility: visible;
  }
  .detail-col {
    position: fixed;
    top: auto;
    left: 0;
    width: 100%;
    height: min(85dvh, 640px);
    border-left: 0;
    border-radius: 22px 22px 0 0;
    transform: translateY(100%);
    padding-bottom: env(safe-area-inset-bottom);
  }
  .main {
    overflow: visible;
    padding: 0 1rem calc(2rem + env(safe-area-inset-bottom));
  }
  .topbar {
    position: sticky;
    top: 0;
    z-index: 5;
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin: 0 -1rem 0.75rem;
    padding: calc(0.5rem + env(safe-area-inset-top)) 0.6rem 0.5rem;
    background: var(--app-bg);
  }
  .topbar-title {
    font-weight: 700;
    font-size: 1.05rem;
  }
  .add-icon {
    background: var(--primary);
    color: var(--primary-text);
    border-radius: 50%;
  }
  .page-header {
    margin: 1.25rem 0 1rem;
  }
  .new-btn {
    display: none; /* the + in the top bar replaces it on phones */
  }
  h1 {
    font-size: 1.45rem;
  }
  .task-dialog {
    margin: auto auto 0;
    width: 100%;
    max-width: 100%;
    border-radius: 22px 22px 0 0;
    padding-bottom: calc(1.5rem + env(safe-area-inset-bottom));
  }
}
</style>
