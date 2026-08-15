import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function ProtectedRoute() {
  const { auth } = useAuth()

  if (!auth || auth.role !== 'ADMIN') {
    return <Navigate to="/login" replace />
  }

  return <Outlet />
}
