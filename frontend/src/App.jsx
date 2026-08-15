import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import Layout from './components/Layout'
import Login from './pages/Login'
import Products from './pages/Products'
import Categories from './pages/Categories'
import Orders from './pages/Orders'

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<Login />} />

        <Route element={<ProtectedRoute />}>
          <Route element={<Layout />}>
            <Route index element={<Navigate to="/products" replace />} />
            <Route path="/products" element={<Products />} />
            <Route path="/categories" element={<Categories />} />
            <Route path="/orders" element={<Orders />} />
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/products" replace />} />
      </Routes>
    </AuthProvider>
  )
}
