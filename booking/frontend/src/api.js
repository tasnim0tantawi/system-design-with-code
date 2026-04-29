const BASE = '/api'

function getToken() {
  return localStorage.getItem('token')
}

async function req(method, path, body) {
  const token = getToken()
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
  })
  const data = await res.json().catch(() => null)
  if (!res.ok) throw new Error(data?.message || `HTTP ${res.status}`)
  return data
}

export const auth = {
  login: (email, password) => req('POST', '/auth/login', { email, password }),
  registerUser: (email, password, name) => req('POST', '/auth/register/user', { email, password, name }),
  registerManager: (email, password, name) => req('POST', '/auth/register/manager', { email, password, name }),
}

export const hotels = {
  list: () => req('GET', '/hotels'),
  get: (id) => req('GET', `/hotels/${id}`),
  search: (params) => {
    const qs = new URLSearchParams(
      Object.fromEntries(Object.entries(params).filter(([, v]) => v))
    ).toString()
    return req('GET', `/hotels/search${qs ? `?${qs}` : ''}`)
  },
  availability: (id, checkIn, checkOut) =>
    req('GET', `/hotels/${id}/availability?check_in=${checkIn}&check_out=${checkOut}`),
  reviews: (id) => req('GET', `/hotels/${id}/reviews`),
  reviewSummary: (id) => req('GET', `/hotels/${id}/reviews/summary`),
}

export const bookings = {
  create: (hotelId, roomId, checkIn, checkOut) =>
    req('POST', `/hotels/${hotelId}/bookings`, { roomId, checkIn, checkOut }),
  pay: (bookingId) => req('POST', `/bookings/${bookingId}/payment`),
  get: (bookingId) => req('GET', `/bookings/${bookingId}`),
  listForUser: (userId) => req('GET', `/users/${userId}/bookings`),
  cancel: (bookingId) => req('DELETE', `/bookings/${bookingId}`),
}

export const reviews = {
  create: (hotelId, bookingId, rating, comment) =>
    req('POST', `/hotels/${hotelId}/reviews`, { bookingId, rating, comment }),
}

export const notifications = {
  list: (userId) => req('GET', `/users/${userId}/notifications`),
  markRead: (userId, notifId) => req('POST', `/users/${userId}/notifications/${notifId}/read`),
}
