import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
})

// Attach the JWT (if we have one) to every outgoing request.
client.interceptors.request.use((config) => {
  const stored = localStorage.getItem('auth')
  if (stored) {
    const { token } = JSON.parse(stored)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
  }
  return config
})

// If the backend says our token is invalid/expired, drop the stored session
// and bounce to login so the user isn't stuck looking at stale UI.
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('auth')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default client
