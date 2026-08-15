import { createContext, useContext, useState } from 'react'
import client from '../api/client'

const AuthContext = createContext(null)

function loadStoredAuth() {
  const stored = localStorage.getItem('auth')
  return stored ? JSON.parse(stored) : null
}

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(loadStoredAuth)

  async function login(email, password) {
    const { data } = await client.post('/auth/login', { email, password })
    // data = { token, email, role }
    if (data.role !== 'ADMIN') {
      throw new Error('This account does not have admin access.')
    }
    localStorage.setItem('auth', JSON.stringify(data))
    setAuth(data)
    return data
  }

  function logout() {
    localStorage.removeItem('auth')
    setAuth(null)
  }

  return (
    <AuthContext.Provider value={{ auth, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
