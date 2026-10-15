/**
 * All HTTP calls for the Task resource live here, so components never touch fetch directly.
 * Base URL, headers and error parsing are handled in one place (mirrors the backend's controller layer).
 */
const BASE_URL = '/api/v1/tasks'

/**
 * Small fetch wrapper: sends/receives JSON and turns any non-2xx response into a thrown Error.
 * The backend's ErrorResponse ({ status, message, details[] }) is copied onto the Error so a form
 * can show the exact validation messages from Bean Validation.
 */
async function request(path = '', options = {}) {
  let res
  try {
    res = await fetch(`${BASE_URL}${path}`, {
      ...options,
      headers: { 'Content-Type': 'application/json', ...options.headers },
    })
  } catch {
    // fetch only rejects on network failure (backend down, proxy can't connect), not on 4xx/5xx.
    // `code` lets the UI show this in the current language; the api layer knows nothing about i18n.
    const error = new Error('Cannot reach the server. Is the backend running?')
    error.code = 'offline'
    error.details = []
    throw error
  }

  if (res.status === 204) return null

  const body = await res.json().catch(() => null)
  // Vite's proxy answers 500 with an empty body when Spring Boot isn't running.
  if (res.status >= 500 && !body) {
    const error = new Error('Cannot reach the server. Is the backend running?')
    error.code = 'offline'
    error.status = res.status
    error.details = []
    throw error
  }
  if (!res.ok) {
    const error = new Error(body?.message ?? `Request failed: ${res.status}`)
    error.status = res.status
    error.details = body?.details ?? []
    throw error
  }
  return body
}

export function getTasks() {
  return request()
}

export function createTask(payload) {
  return request('', { method: 'POST', body: JSON.stringify(payload) })
}

export function updateTask(id, payload) {
  return request(`/${id}`, { method: 'PUT', body: JSON.stringify(payload) })
}

export function toggleTaskComplete(id) {
  return request(`/${id}/toggle`, { method: 'PATCH' })
}

export function updateTaskProgress(id, progress) {
  return request(`/${id}/progress`, { method: 'PATCH', body: JSON.stringify({ progress }) })
}

/** dueDate is a "yyyy-MM-dd" string (LocalDate on the backend). */
export function rescheduleTask(id, dueDate) {
  return request(`/${id}/reschedule`, { method: 'PATCH', body: JSON.stringify({ dueDate }) })
}

/** [{ taskId, type: 'OVERDUE' | 'CARRY_OVER' | 'FOCUS' | 'SPLIT', suggestedDate }] */
export function getRecommendations() {
  return request('/recommendations')
}

export function deleteTask(id) {
  return request(`/${id}`, { method: 'DELETE' })
}
