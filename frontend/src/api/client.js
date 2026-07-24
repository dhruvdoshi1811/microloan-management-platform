const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

function errorMessageFrom(body, status) {
  if (!body) return `Request failed with status ${status}`
  if (body.fieldErrors && Object.keys(body.fieldErrors).length > 0) {
    return Object.values(body.fieldErrors).join('; ')
  }
  return body.message || `Request failed with status ${status}`
}

async function request(path, { method = 'GET', body, token } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  const text = await response.text()
  const data = text ? JSON.parse(text) : null

  if (!response.ok) {
    throw new Error(errorMessageFrom(data, response.status))
  }

  return data
}

export const api = {
  get: (path, token) => request(path, { token }),
  post: (path, body, token) => request(path, { method: 'POST', body, token }),
}
