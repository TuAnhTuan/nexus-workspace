<script setup>
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'
import { useSettings } from '@/composables/useSettings'
import { formatDay, relativeDayKey } from '@/utils/formatDate'

/**
 * Shows the backend's suggestions. The backend sends codes (type + taskId + suggestedDate);
 * this component turns them into sentences in the current language and offers one-click actions.
 * Nothing changes until the user clicks — suggestions, not automation.
 */
const props = defineProps({
  recommendations: { type: Array, required: true },
  tasks: { type: Array, required: true },
  busy: { type: Object, default: () => ({}) },
})
const emit = defineEmits(['reschedule', 'open', 'dismiss', 'apply-all'])
const { t, locale } = useSettings()

function dayText(day) {
  const key = relativeDayKey(day)
  return key ? t(`day.${key}`).toLowerCase() : formatDay(day, locale.value)
}

// Join each suggestion with its task (skip suggestions whose task no longer exists locally).
const items = computed(() =>
  props.recommendations
    .map((rec) => ({ rec, task: props.tasks.find((x) => x.id === rec.taskId) }))
    .filter((x) => x.task)
    .map(({ rec, task }) => ({
      key: `${rec.taskId}-${rec.type}`,
      rec,
      task,
      moveTo: rec.suggestedDate,
      text: t(`rec.${rec.type}`, {
        title: task.title,
        pct: task.progress,
        n: task.postponedCount,
        date: dayText(task.dueDate),
      }),
    })),
)

const movable = computed(() => items.value.filter((x) => x.moveTo))
</script>

<template>
  <section v-if="items.length" class="panel" aria-labelledby="rec-title">
    <header>
      <h2 id="rec-title"><AppIcon name="bulb" :size="17" />{{ t('rec.title') }}</h2>
      <button
        v-if="movable.length > 1"
        type="button"
        class="btn btn-primary apply-all"
        @click="emit('apply-all', movable.map((x) => ({ taskId: x.rec.taskId, date: x.moveTo })))"
      >
        {{ t('rec.applyAll') }}
      </button>
    </header>

    <TransitionGroup tag="ul" name="rec">
      <li v-for="item in items" :key="item.key" :class="item.rec.type.toLowerCase()">
        <p>{{ item.text }}</p>
        <div class="actions">
          <button
            v-if="item.moveTo"
            type="button"
            class="btn btn-primary"
            :disabled="busy[item.rec.taskId]"
            @click="emit('reschedule', item.rec.taskId, item.moveTo)"
          >
            <AppIcon name="arrowRight" :size="14" />{{ t(`rec.${item.rec.type}_action`) }}
          </button>
          <button v-else type="button" class="btn" @click="emit('open', item.rec.taskId)">
            {{ t('rec.open') }}
          </button>
          <button
            type="button"
            class="icon-btn"
            :aria-label="t('rec.dismiss')"
            :title="t('rec.dismiss')"
            @click="emit('dismiss', item.key)"
          >
            <AppIcon name="x" :size="16" />
          </button>
        </div>
      </li>
    </TransitionGroup>
  </section>
</template>

<style scoped>
.panel {
  margin-bottom: 1.5rem;
  padding: 1rem;
  border-radius: var(--radius);
  background: var(--warn-soft);
}
header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}
h2 {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  margin: 0;
  font-size: 0.95rem;
  color: var(--warn);
}
.apply-all {
  padding: 0.35rem 0.8rem;
  font-size: 0.8rem;
}
ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
li {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.7rem 0.8rem;
  border-radius: var(--radius-sm);
  background: var(--surface);
  border-left: 4px solid var(--warn);
}
li.overdue {
  border-left-color: var(--danger);
}
li.split {
  border-left-color: var(--primary);
}
p {
  flex: 1;
  margin: 0;
  font-size: 0.875rem;
  overflow-wrap: anywhere;
}
.actions {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  flex-shrink: 0;
}
.actions .btn {
  padding: 0.4rem 0.75rem;
  font-size: 0.8rem;
}

.rec-enter-active,
.rec-leave-active {
  transition: all 0.2s ease;
}
.rec-enter-from,
.rec-leave-to {
  opacity: 0;
  transform: translateX(8px);
}

@media (max-width: 640px) {
  li {
    flex-direction: column;
    align-items: stretch;
  }
  .actions {
    justify-content: flex-end;
  }
}
</style>
