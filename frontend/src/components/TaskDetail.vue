<script setup>
import { computed, ref, useId, watch } from 'vue'
import AppIcon from './AppIcon.vue'
import { useSettings } from '@/composables/useSettings'
import { addDays, formatDate, formatDay, isOverdue, relativeDayKey, today } from '@/utils/formatDate'

/**
 * Right panel: full details of the selected task + progress, scheduling, toggle / delete.
 * "Edit" only asks the page to open the shared task dialog (same popup as "New task").
 * Like the other components, it only emits — the page makes the API calls.
 */
const props = defineProps({
  task: { type: Object, default: null },
  busy: { type: Boolean, default: false },
  errorText: { type: String, default: '' },
})
const emit = defineEmits(['toggle', 'edit', 'delete', 'close', 'progress', 'reschedule'])
const { t, locale } = useSettings()
const id = useId()

// Local copy of the slider value: it moves freely while dragging, and is only sent to the
// backend on `change` (when the finger/mouse is released) — one request instead of dozens.
const progressDraft = ref(0)

// Resync the slider when another task is selected or the server returns a new version.
watch(
  () => [props.task?.id, props.task?.updatedAt],
  () => {
    progressDraft.value = props.task?.progress ?? 0
  },
  { immediate: true },
)

function commitProgress(value) {
  progressDraft.value = value
  if (value !== props.task.progress) emit('progress', value)
}

const quickSteps = [0, 25, 50, 75, 100]

const dueLabel = computed(() => {
  const day = props.task?.dueDate
  if (!day) return t('day.none')
  const key = relativeDayKey(day)
  return key ? `${t(`day.${key}`)} · ${formatDay(day, locale.value)}` : formatDay(day, locale.value)
})
const overdue = computed(() => props.task && !props.task.completed && isOverdue(props.task.dueDate))
const todayStr = computed(() => today())
const tomorrowStr = computed(() => addDays(today(), 1))

function onPickDate(e) {
  if (e.target.value) emit('reschedule', e.target.value)
}

function onDelete() {
  if (window.confirm(t('detail.confirmDelete', { title: props.task.title }))) emit('delete')
}
</script>

<template>
  <div class="detail">
    <div v-if="task" class="detail-top">
      <button type="button" class="icon-btn close" :aria-label="t('detail.close')" @click="emit('close')">
        <AppIcon name="x" />
      </button>
    </div>

    <p v-if="!task" class="placeholder">{{ t('detail.empty') }}</p>

    <template v-else>
      <div class="sheet">
        <span class="status" :class="task.completed ? 'done' : task.progress > 0 ? 'doing' : ''">
          {{ task.completed ? t('board.completed') : task.progress > 0 ? t('board.inProgress') : t('board.todo') }}
        </span>
        <h2>{{ task.title }}</h2>
        <p class="description" :class="{ muted: !task.description }">
          {{ task.description || t('detail.noDescription') }}
        </p>

        <dl>
          <dt>{{ t('detail.created') }}</dt>
          <dd>{{ formatDate(task.createdAt, locale) }}</dd>
          <dt>{{ t('detail.updated') }}</dt>
          <dd>{{ formatDate(task.updatedAt, locale) }}</dd>
        </dl>

        <div class="row-actions">
          <button type="button" class="btn" :disabled="busy" @click="emit('edit')">
            <AppIcon name="edit" :size="15" />{{ t('detail.edit') }}
          </button>
          <button type="button" class="btn btn-danger" :disabled="busy" @click="onDelete">
            <AppIcon name="trash" :size="15" />{{ t('detail.delete') }}
          </button>
        </div>
      </div>

      <!-- Progress -->
      <section class="block">
        <div class="block-head">
          <label :for="`${id}-progress`">{{ t('detail.progress') }}</label>
          <span class="pct">{{ progressDraft }}%</span>
        </div>
        <input
          :id="`${id}-progress`"
          v-model.number="progressDraft"
          type="range"
          min="0"
          max="100"
          step="5"
          class="slider"
          :style="{ '--pct': `${progressDraft}%` }"
          :disabled="busy"
          :aria-label="t('detail.progressLabel')"
          @change="commitProgress(progressDraft)"
        />
        <div class="quick">
          <button
            v-for="step in quickSteps"
            :key="step"
            type="button"
            :class="{ active: task.progress === step }"
            :disabled="busy"
            @click="commitProgress(step)"
          >
            {{ step }}%
          </button>
        </div>
      </section>

      <!-- Scheduling -->
      <section class="block">
        <div class="block-head">
          <span class="block-title">{{ t('detail.schedule') }}</span>
          <span v-if="task.postponedCount > 0" class="postponed">
            {{ t('detail.postponed', { n: task.postponedCount }) }}
          </span>
        </div>
        <p class="due" :class="{ overdue }"><AppIcon name="calendar" :size="15" />{{ dueLabel }}</p>
        <div class="quick">
          <button
            type="button"
            :class="{ active: task.dueDate === todayStr }"
            :disabled="busy || task.dueDate === todayStr"
            @click="emit('reschedule', todayStr)"
          >
            {{ t('detail.moveToday') }}
          </button>
          <button
            type="button"
            :class="{ active: task.dueDate === tomorrowStr }"
            :disabled="busy || task.dueDate === tomorrowStr"
            @click="emit('reschedule', tomorrowStr)"
          >
            {{ t('detail.moveTomorrow') }}
          </button>
          <label class="date-pick">
            <span class="sr-only">{{ t('detail.pickDate') }}</span>
            <input type="date" :value="task.dueDate ?? ''" :disabled="busy" @change="onPickDate" />
          </label>
        </div>
      </section>

      <p v-if="errorText" class="error" role="alert">
        {{ errorText }}
      </p>

      <button
        type="button"
        class="btn btn-primary mark"
        :class="{ undo: task.completed }"
        :disabled="busy"
        @click="emit('toggle')"
      >
        <AppIcon :name="task.completed ? 'circle' : 'check'" :size="16" />
        {{ task.completed ? t('detail.markTodo') : t('detail.markDone') }}
      </button>
    </template>
  </div>
</template>

<style scoped>
.detail {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  height: 100%;
  padding: 1.25rem;
  overflow-y: auto;
}
.detail-top {
  display: flex;
  justify-content: flex-end;
  margin-bottom: -0.5rem;
}
.placeholder {
  margin: auto 0;
  text-align: center;
  color: var(--muted);
  padding: 2rem 1rem;
}
.sheet {
  padding: 1.1rem;
  border-radius: var(--radius);
  background: var(--surface-2);
}
.status {
  display: inline-block;
  padding: 0.15rem 0.6rem;
  border-radius: 999px;
  background: var(--todo-soft);
  color: var(--todo);
  font-size: 0.75rem;
  font-weight: 700;
}
.status.doing {
  background: var(--warn-soft);
  color: var(--warn);
}
.status.done {
  background: var(--done-soft);
  color: var(--done);
}
h2 {
  margin: 0.6rem 0 0.4rem;
  font-size: 1.15rem;
  overflow-wrap: anywhere;
}
.description {
  margin: 0;
  font-size: 0.9rem;
  white-space: pre-line;
  overflow-wrap: anywhere;
}
.description.muted {
  color: var(--muted);
  font-style: italic;
}
dl {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.35rem 1rem;
  margin: 1rem 0;
  padding-top: 1rem;
  border-top: 1px solid var(--border);
  font-size: 0.85rem;
}
dt {
  color: var(--muted);
}
dd {
  margin: 0;
}
.row-actions {
  display: flex;
  gap: 0.5rem;
}
.row-actions .btn {
  flex: 1;
}

.block {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}
.block-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.block-head label,
.block-title {
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--muted);
}
.pct {
  font-size: 1.1rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.postponed {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--warn);
}

/* Range input: track filled up to the current value via the --pct custom property. */
.slider {
  width: 100%;
  height: 28px;
  margin: 0;
  background: transparent;
  appearance: none;
  -webkit-appearance: none;
}
.slider::-webkit-slider-runnable-track {
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(to right, var(--primary) var(--pct), var(--surface-2) var(--pct));
}
.slider::-moz-range-track {
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(to right, var(--primary) var(--pct), var(--surface-2) var(--pct));
}
.slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 22px;
  height: 22px;
  margin-top: -7px;
  border-radius: 50%;
  border: 3px solid var(--surface);
  background: var(--primary);
  box-shadow: 0 1px 4px rgb(0 0 0 / 0.25);
}
.slider::-moz-range-thumb {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 3px solid var(--surface);
  background: var(--primary);
}

.quick {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}
.quick button {
  flex: 1;
  min-width: 3rem;
  padding: 0.4rem 0.5rem;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--surface);
  font-size: 0.8rem;
  font-weight: 600;
  white-space: nowrap;
}
.quick button.active {
  border-color: var(--primary);
  background: var(--primary-soft);
  color: var(--primary);
  opacity: 1;
}
.date-pick {
  flex: 1 1 100%; /* the date input gets its own row so "Today / Tomorrow" never wrap */
  display: flex;
}
.date-pick input {
  width: 100%;
  min-width: 0;
  padding: 0.35rem 0.5rem;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--surface);
  font-size: 0.8rem;
}
.due {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin: 0;
  font-weight: 600;
}
.due.overdue {
  color: var(--danger);
}

.error {
  margin: 0;
  padding: 0.6rem 0.8rem;
  border-radius: var(--radius-sm);
  background: var(--danger-soft);
  color: var(--danger);
  font-size: 0.85rem;
}
.mark {
  margin-top: auto;
  padding: 0.8rem;
}
.mark.undo {
  background: var(--surface-2);
  border-color: var(--border);
  color: var(--text);
}
</style>
