<script setup>
import { computed, reactive, ref, useId } from 'vue'
import { useSettings } from '@/composables/useSettings'
import { today } from '@/utils/formatDate'

/**
 * Used for both creating (no initialTask) and editing (initialTask given).
 * The form only collects + validates input and emits it; the parent decides which API to call.
 */
const props = defineProps({
  initialTask: { type: Object, default: null },
  submitting: { type: Boolean, default: false },
  errors: { type: Array, default: () => [] },
  autofocus: { type: Boolean, default: false },
})
const emit = defineEmits(['submit', 'cancel'])
const { t } = useSettings()

// Limits mirror TaskRequest's Bean Validation. The backend is still the source of truth;
// this just gives instant feedback without a round trip.
const TITLE_MAX = 255
const DESCRIPTION_MAX = 2000

const isEdit = computed(() => props.initialTask !== null)
const form = reactive({
  title: props.initialTask?.title ?? '',
  description: props.initialTask?.description ?? '',
  // New tasks are planned for today by default — the app is built around "what do I do today".
  dueDate: props.initialTask ? (props.initialTask.dueDate ?? '') : today(),
})
// Store message *keys*, not text, so switching language re-translates errors already on screen.
const localErrors = ref([])

// useId() gives each form instance unique ids, so <label for> points at the right input
// even when the create dialog and the edit form exist at the same time.
const id = useId()

function validate() {
  const errs = []
  if (!form.title.trim()) errs.push({ key: 'form.titleRequired' })
  if (form.title.length > TITLE_MAX) errs.push({ key: 'form.titleTooLong', params: { max: TITLE_MAX } })
  if (form.description.length > DESCRIPTION_MAX) {
    errs.push({ key: 'form.descriptionTooLong', params: { max: DESCRIPTION_MAX } })
  }
  return errs
}

function onSubmit() {
  localErrors.value = validate()
  if (localErrors.value.length) return
  emit('submit', {
    title: form.title.trim(),
    description: form.description.trim() || null,
    dueDate: form.dueDate || null, // <input type="date"> already gives "yyyy-MM-dd" = Java LocalDate
  })
}

// Exposed so the parent can clear the form only after the create request succeeded —
// if it fails, the user's input stays put.
function reset() {
  form.title = ''
  form.description = ''
  form.dueDate = today()
  localErrors.value = []
}
defineExpose({ reset })

const shownErrors = computed(() =>
  localErrors.value.length ? localErrors.value.map((e) => t(e.key, e.params)) : props.errors,
)
</script>

<template>
  <form class="task-form" novalidate @submit.prevent="onSubmit">
    <div class="field">
      <label :for="`${id}-title`">{{ t('form.title') }}</label>
      <input
        :id="`${id}-title`"
        v-model="form.title"
        type="text"
        :placeholder="t('form.titlePlaceholder')"
        :autofocus="autofocus"
        maxlength="300"
      />
    </div>
    <div class="field">
      <label :for="`${id}-description`">{{ t('form.description') }}</label>
      <textarea
        :id="`${id}-description`"
        v-model="form.description"
        rows="4"
        :placeholder="t('form.descriptionPlaceholder')"
      />
    </div>
    <div class="field">
      <label :for="`${id}-due`">{{ t('form.dueDate') }}</label>
      <input :id="`${id}-due`" v-model="form.dueDate" type="date" />
    </div>

    <ul v-if="shownErrors.length" class="errors" role="alert">
      <li v-for="err in shownErrors" :key="err">{{ err }}</li>
    </ul>

    <div class="actions">
      <button type="button" class="btn btn-ghost" @click="emit('cancel')">{{ t('form.cancel') }}</button>
      <button type="submit" class="btn btn-primary" :disabled="submitting">
        {{ submitting ? t('form.saving') : isEdit ? t('form.save') : t('form.add') }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.task-form {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--muted);
}
input,
textarea {
  padding: 0.65rem 0.8rem;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface-2);
  font-size: 16px; /* ≥16px stops iOS Safari from zooming in when the field is focused */
}
input:focus,
textarea:focus {
  outline: none;
  border-color: var(--primary);
  background: var(--surface);
}
textarea {
  resize: vertical;
}
.errors {
  margin: 0;
  padding: 0.6rem 0.8rem 0.6rem 1.8rem;
  border-radius: var(--radius-sm);
  background: var(--danger-soft);
  color: var(--danger);
  font-size: 0.85rem;
}
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>
