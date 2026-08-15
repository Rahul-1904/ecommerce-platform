import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Layout() {
  const { auth, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">Ecommerce Admin</div>
        <nav>
          <NavLink to="/products" className={({ isActive }) => (isActive ? 'active' : '')}>
            Products
          </NavLink>
          <NavLink to="/categories" className={({ isActive }) => (isActive ? 'active' : '')}>
            Categories
          </NavLink>
          <NavLink to="/orders" className={({ isActive }) => (isActive ? 'active' : '')}>
            Orders
          </NavLink>
        </nav>
        <div className="user-info">
          <span>{auth?.email} ({auth?.role})</span>
          <button onClick={handleLogout}>Logout</button>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
