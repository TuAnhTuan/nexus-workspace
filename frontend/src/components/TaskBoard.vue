<script setup>
import { computed } from 'vue'
import TaskCard from './TaskCard.vue'
import { useSettings } from '@/composables/useSettings'

/**
 * Kanban-style columns derived from progress:
 * 0% → To do, 1–99% → In progress, 100% → Completed.
 * `columns` limits which columns are shown (the sidebar's "Not done" / "Completed" filters).
 */
const props = defineProps({
  tasks: { type: Array, required: true },
  columns: { type: Array, default: () => ['todo', 'inProgress', 'completed'] },
  selectedId: { type: Number, default: null },
  busy: { type: Object, default: () => ({}) },
})
const emit = defineEmits(['select', 'toggle'])
const { t } = useSettings()

function columnOf(task) {
  if (task.completed) return 'completed'
  return task.progress > 0 ? 'inProgress' : 'todo'
}

// Sort inside a column: earliest due day first, unscheduled last.
function byDueDate(a, b) {
  return (a.dueDate ?? '9999-12-31').localeCompare(b.dueDate ?? '9999-12-31')
}

const visibleColumns = computed(() =>
  props.columns.map((id) => ({
    id,
    items: props.tasks.filter((x) => columnOf(x) === id).sort(byDueDate),
  })),
)
</script>

<template>
  <div class="board" :style="{ '--cols': visibleColumns.length }">
    <section v-for="col in visibleColumns" :key="col.id" class="column" :class="col.id">
      <header class="column-header">
        <h3>{{ t(`board.${col.id}`) }}</h3>
        <span class="badge">{{ col.items.length }}</span>
      </header>

      <TransitionGroup tag="div" name="list" class="cards">
        <!-- key by task.id (stable), never the index, so Vue moves the right card between columns -->
        <TaskCard
          v-for="task in col.items"
          :key="task.id"
          :task="task"
          :selected="task.id === selectedId"
          :busy="!!busy[task.id]"
          @select="emit('select', task)"
          @toggle="emit('toggle', task)"
        />
      </TransitionGroup>
      <p v-if="!col.items.length" class="empty">{{ t('board.empty') }}</p>
    </section>
  </div>
</template>

<style scoped>
.board {
  display: grid;
  grid-template-columns: repeat(var(--cols), minmax(0, 1fr));
  gap: 1rem;
  align-items: start;
}
.column-header {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-bottom: 0.8rem;
  padding: 0.6rem 0.9rem;
  border-radius: var(--radius-sm);
  background: var(--surface);
  border-left: 4px solid var(--todo);
}
.inProgress .column-header {
  border-left-color: var(--warn);
}
.completed .column-header {
  border-left-color: var(--done);
}
h3 {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
}
.badge {
  padding: 0 0.5rem;
  border-radius: 999px;
  background: var(--todo-soft);
  color: var(--todo);
  font-size: 0.75rem;
  font-weight: 700;
}
.inProgress .badge {
  background: var(--warn-soft);
  color: var(--warn);
}
.completed .badge {
  background: var(--done-soft);
  color: var(--done);
}
.cards {
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}
.empty {
  margin: 0;
  padding: 1.25rem;
  border: 1.5px dashed var(--border);
  border-radius: var(--radius);
  text-align: center;
  color: var(--muted);
  font-size: 0.85rem;
}

/* Animate cards entering/leaving a column. */
.list-enter-active,
.list-leave-active {
  transition: all 0.2s ease;
}
.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateY(6px);
}
.list-leave-active {
  position: absolute;
  visibility: hidden;
}

/* 3 columns get cramped on mid-size screens → stack them. */
@media (max-width: 1024px) {
  .board {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
