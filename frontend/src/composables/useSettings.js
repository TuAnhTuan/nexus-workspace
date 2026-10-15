import { ref, watchEffect } from 'vue'
import { messages } from '@/i18n/messages'

/**
 * User preferences (language, font, theme) shared by the whole app.
 *
 * The refs are created once at module level, so every component that calls useSettings()
 * gets the *same* refs. That's enough shared state for 3 settings — no Pinia needed yet.
 * Values are saved to localStorage so they survive a reload.
 */
export const LOCALES = [
  { code: 'en', label: 'English' },
  { code: 'vi', label: 'Tiếng Việt' },
]

// All four have full Vietnamese glyph support (loaded in index.html).
export const FONTS = [
  { id: 'jakarta', label: 'Plus Jakarta Sans', stack: "'Plus Jakarta Sans', system-ui, sans-serif" },
  { id: 'be-vietnam', label: 'Be Vietnam Pro', stack: "'Be Vietnam Pro', system-ui, sans-serif" },
  { id: 'nunito', label: 'Nunito', stack: "'Nunito', system-ui, sans-serif" },
  { id: 'lora', label: 'Lora', stack: "'Lora', Georgia, serif" },
]

function load(key, allowed, fallback) {
  try {
    const value = localStorage.getItem(key)
    return allowed.includes(value) ? value : fallback
  } catch {
    return fallback // storage can be blocked (private mode) — just use the default
  }
}

function save(key, value) {
  try {
    localStorage.setItem(key, value)
  } catch {
    /* ignore */
  }
}

const browserLocale = navigator.language?.startsWith('vi') ? 'vi' : 'en'
const prefersDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches

const locale = ref(load('locale', LOCALES.map((l) => l.code), browserLocale))
const font = ref(load('font', FONTS.map((f) => f.id), 'jakarta'))
const theme = ref(load('theme', ['light', 'dark'], prefersDark ? 'dark' : 'light'))

// watchEffect re-runs whenever a ref it reads changes: persist + apply to <html>.
watchEffect(() => {
  save('locale', locale.value)
  document.documentElement.lang = locale.value
})
watchEffect(() => {
  save('font', font.value)
  const stack = FONTS.find((f) => f.id === font.value).stack
  document.documentElement.style.setProperty('--font', stack)
})
watchEffect(() => {
  save('theme', theme.value)
  document.documentElement.dataset.theme = theme.value
})

/** t('page.progress', { done: 1, total: 3 }) → looks up the key and fills in {placeholders}. */
function t(key, params = {}) {
  const text = key.split('.').reduce((obj, part) => obj?.[part], messages[locale.value]) ?? key
  return text.replace(/\{(\w+)\}/g, (_, name) => params[name] ?? `{${name}}`)
}

export function useSettings() {
  return { locale, font, theme, t }
}
