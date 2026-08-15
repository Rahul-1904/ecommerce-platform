import axios from 'axios'

// In dev, VITE_API_URL is unset and requests go to relative '/api', which
// vite.config.js proxies to the local backend. In production there's no dev
// server to proxy through, so VITE_API_URL must point at the deployed
// backend's real URL (set as a build-time env var on the hosting platform).
const baseURL = import.meta.env.VITE_API_URL ? `${import.meta.env.VITE_API_URL}/api` : '/api'

const client = axios.create({ baseURL })

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
