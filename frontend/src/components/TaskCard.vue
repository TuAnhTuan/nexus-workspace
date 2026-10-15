<script setup>
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'
import { useSettings } from '@/composables/useSettings'
import { formatDay, isOverdue, relativeDayKey } from '@/utils/formatDate'

/** One task on the board. Clicking it selects it; the round checkbox toggles it. */
const props = defineProps({
  task: { type: Object, required: true },
  selected: { type: Boolean, default: false },
  busy: { type: Boolean, default: false },
})
const emit = defineEmits(['select', 'toggle'])
const { t, locale } = useSettings()

const dueLabel = computed(() => {
  const key = relativeDayKey(props.task.dueDate)
  return key ? t(`day.${key}`) : formatDay(props.task.dueDate, locale.value)
})
const overdue = computed(() => !props.task.completed && isOverdue(props.task.dueDate))
</script>

<template>
  <article class="card" :class="{ selected, done: task.completed }">
    <button
      type="button"
      class="check"
      :disabled="busy"
      :aria-label="task.completed ? t('detail.markTodo') : t('detail.markDone')"
      :aria-pressed="task.completed"
      @click="emit('toggle')"
    >
      <AppIcon v-if="task.completed" name="check" :size="14" />
    </button>

    <!-- The whole card body is one real <button> so it works with keyboard + screen readers. -->
    <button type="button" class="body" @click="emit('select')">
      <span class="title">{{ task.title }}</span>
      <span v-if="task.description" class="description">{{ task.description }}</span>

      <span v-if="!task.completed && task.progress > 0" class="progress">
        <span class="track"><span class="fill" :style="{ width: `${task.progress}%` }" /></span>
        <span class="pct">{{ task.progress }}%</span>
      </span>

      <span class="meta">
        <span v-if="task.dueDate" class="due" :class="{ overdue }">
          <AppIcon name="calendar" :size="13" />{{ dueLabel }}
        </span>
        <span v-if="task.postponedCount > 0" class="postponed" :title="t('detail.postponed', { n: task.postponedCount })">
          ↻ {{ task.postponedCount }}
        </span>
      </span>
    </button>
  </article>
</template>

<style scoped>
.card {
  display: flex;
  gap: 0.75rem;
  padding: 0.9rem;
  border-radius: var(--radius);
  border: 1.5px solid transparent;
  background: var(--surface);
  box-shadow: var(--shadow);
  transition:
    border-color 0.15s,
    transform 0.15s;
}
.card:hover {
  transform: translateY(-1px);
}
.card.selected {
  border-color: var(--primary);
}
.check {
  flex-shrink: 0;
  display: grid;
  place-items: center;
  width: 1.35rem;
  height: 1.35rem;
  margin-top: 0.1rem;
  padding: 0;
  border-radius: 50%;
  border: 2px solid var(--border);
  background: transparent;
  color: #fff;
}
.check:hover {
  border-color: var(--done);
}
.done .check {
  border-color: var(--done);
  background: var(--done);
}
.body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  padding: 0;
  border: 0;
  background: transparent;
  text-align: left;
}
.title {
  font-weight: 600;
  overflow-wrap: anywhere;
}
.done .title {
  text-decoration: line-through;
  color: var(--muted);
}
.description {
  color: var(--muted);
  font-size: 0.85rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.progress {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.15rem;
}
.track {
  flex: 1;
  height: 6px;
  border-radius: 999px;
  background: var(--surface-2);
  overflow: hidden;
}
.fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--warn);
  transition: width 0.3s ease;
}
.pct {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--warn);
  font-variant-numeric: tabular-nums;
}
.meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.15rem;
  font-size: 0.75rem;
  color: var(--muted);
}
.due {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.1rem 0.5rem;
  border-radius: 999px;
  background: var(--surface-2);
  font-weight: 600;
}
.due.overdue {
  background: var(--danger-soft);
  color: var(--danger);
}
.postponed {
  font-weight: 600;
}
</style>
