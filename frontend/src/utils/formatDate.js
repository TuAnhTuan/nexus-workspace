/**
 * Date helpers. Pure functions, no Vue imports.
 * Due dates travel as "yyyy-MM-dd" strings (Java LocalDate) — a calendar day with no time zone —
 * so we build/compare them as local-day strings, never via toISOString() (that's UTC and can
 * shift the day by one in Vietnam, UTC+7).
 */

function intlLocale(locale) {
  return locale === 'vi' ? 'vi-VN' : 'en-GB'
}

/** Formats an ISO instant from the backend, e.g. "15 Oct 2026, 09:30". */
export function formatDate(iso, locale) {
  if (!iso) return ''
  return new Intl.DateTimeFormat(intlLocale(locale), {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(iso))
}

/** Local calendar day as "yyyy-MM-dd". */
export function toDayString(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export function today() {
  return toDayString(new Date())
}

export function addDays(day, n) {
  const [y, m, d] = day.split('-').map(Number)
  return toDayString(new Date(y, m - 1, d + n))
}

/** "yyyy-MM-dd" strings compare correctly as plain strings. */
export function isOverdue(day) {
  return !!day && day < today()
}

/**
 * Relative label key for a due day: 'today' | 'tomorrow' | 'yesterday' | null (→ show the date).
 * Returned as an i18n key suffix so the caller translates it.
 */
export function relativeDayKey(day) {
  if (!day) return null
  const t = today()
  if (day === t) return 'today'
  if (day === addDays(t, 1)) return 'tomorrow'
  if (day === addDays(t, -1)) return 'yesterday'
  return null
}

/** Formats a "yyyy-MM-dd" day, e.g. "Fri, 16 Oct". */
export function formatDay(day, locale) {
  if (!day) return ''
  const [y, m, d] = day.split('-').map(Number)
  return new Intl.DateTimeFormat(intlLocale(locale), {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
  }).format(new Date(y, m - 1, d))
}
