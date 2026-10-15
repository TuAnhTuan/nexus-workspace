<script setup>
import AppIcon from './AppIcon.vue'
import { FONTS, LOCALES, useSettings } from '@/composables/useSettings'

/**
 * Left column: status filters + settings (language, font, theme).
 * `filter` uses v-model (prop `modelValue` + event `update:modelValue`) so the page owns the value.
 * Settings come straight from useSettings() — they're app-wide, so no need to pass them as props.
 */
defineProps({
  modelValue: { type: String, required: true },
  counts: { type: Object, required: true },
})
const emit = defineEmits(['update:modelValue'])
const { t, locale, font, theme } = useSettings()

const filters = [
  { id: 'all', icon: 'list' },
  { id: 'today', icon: 'calendar' },
  { id: 'todo', icon: 'circle' },
  { id: 'completed', icon: 'checkCircle' },
]
</script>

<template>
  <nav class="sidebar">
    <div class="brand">
      <span class="logo"><AppIcon name="check" :size="16" /></span>
      {{ t('appName') }}
    </div>

    <ul class="filters">
      <li v-for="f in filters" :key="f.id">
        <button
          type="button"
          :class="{ active: modelValue === f.id }"
          :aria-current="modelValue === f.id ? 'page' : undefined"
          @click="emit('update:modelValue', f.id)"
        >
          <AppIcon :name="f.icon" />
          <span>{{ t(`nav.${f.id}`) }}</span>
          <span class="count">{{ counts[f.id] }}</span>
        </button>
      </li>
    </ul>

    <section class="settings">
      <h2>{{ t('settings.title') }}</h2>

      <div class="setting">
        <span class="setting-label"><AppIcon name="globe" :size="15" />{{ t('settings.language') }}</span>
        <div class="segmented" role="radiogroup" :aria-label="t('settings.language')">
          <button
            v-for="l in LOCALES"
            :key="l.code"
            type="button"
            role="radio"
            :aria-checked="locale === l.code"
            :class="{ active: locale === l.code }"
            @click="locale = l.code"
          >
            {{ l.code.toUpperCase() }}
          </button>
        </div>
      </div>

      <div class="setting stacked">
        <label class="setting-label" for="font-select">
          <AppIcon name="type" :size="15" />{{ t('settings.font') }}
        </label>
        <select id="font-select" v-model="font">
          <option v-for="f in FONTS" :key="f.id" :value="f.id" :style="{ fontFamily: f.stack }">
            {{ f.label }}
          </option>
        </select>
      </div>

      <div class="setting">
        <span class="setting-label">
          <AppIcon :name="theme === 'dark' ? 'moon' : 'sun'" :size="15" />{{ t('settings.theme') }}
        </span>
        <div class="segmented" role="radiogroup" :aria-label="t('settings.theme')">
          <button
            v-for="mode in ['light', 'dark']"
            :key="mode"
            type="button"
            role="radio"
            :aria-checked="theme === mode"
            :aria-label="t(`settings.${mode}`)"
            :class="{ active: theme === mode }"
            @click="theme = mode"
          >
            <AppIcon :name="mode === 'light' ? 'sun' : 'moon'" :size="15" />
          </button>
        </div>
      </div>
    </section>
  </nav>
</template>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  height: 100%;
  padding: 1.5rem 1rem;
  overflow-y: auto;
}
.brand {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0 0.5rem;
  font-size: 1.15rem;
  font-weight: 700;
}
.logo {
  display: grid;
  place-items: center;
  width: 1.9rem;
  height: 1.9rem;
  border-radius: 9px;
  background: var(--primary);
  color: var(--primary-text);
}
.filters {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}
.filters button {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  width: 100%;
  padding: 0.6rem 0.75rem;
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--muted);
  font-weight: 500;
  text-align: left;
}
.filters button:hover {
  background: var(--surface-2);
  color: var(--text);
}
.filters button.active {
  background: var(--primary-soft);
  color: var(--primary);
  font-weight: 600;
}
.count {
  margin-left: auto;
  font-size: 0.8rem;
  font-variant-numeric: tabular-nums;
}
.settings {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
  padding: 1rem 0.75rem;
  border-radius: var(--radius);
  background: var(--surface-2);
}
h2 {
  margin: 0;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--muted);
}
.setting {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}
.setting.stacked {
  flex-direction: column;
  align-items: stretch;
  gap: 0.4rem;
}
.setting-label {
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  font-size: 0.85rem;
  font-weight: 500;
}
.segmented {
  display: inline-flex;
  padding: 3px;
  border-radius: 9px;
  background: var(--surface);
  border: 1px solid var(--border);
}
.segmented button {
  display: inline-grid;
  place-items: center;
  min-width: 2.2rem;
  padding: 0.25rem 0.5rem;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: var(--muted);
  font-size: 0.75rem;
  font-weight: 700;
}
.segmented button.active {
  background: var(--primary);
  color: var(--primary-text);
}
select {
  width: 100%;
  padding: 0.45rem 0.5rem;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface);
  font-size: 0.85rem;
}
</style>
