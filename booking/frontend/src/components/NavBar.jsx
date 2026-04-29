import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../AuthContext'
import { Hotel, CalendarDays, Bell, LogOut, LogIn } from 'lucide-react'

export default function NavBar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <nav className="bg-white border-b border-gray-200 sticky top-0 z-50">
      <div className="max-w-6xl mx-auto px-4 h-14 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2 font-bold text-blue-600 text-lg">
          <Hotel size={22} />
          BookingApp
        </Link>

        <div className="flex items-center gap-1">
          {user ? (
            <>
              <NavLink to="/" icon={<Hotel size={16} />} label="Hotels" />
              <NavLink to="/bookings" icon={<CalendarDays size={16} />} label="My Bookings" />
              <NavLink to="/notifications" icon={<Bell size={16} />} label="Notifications" />
              <button
                onClick={handleLogout}
                className="ml-2 flex items-center gap-1.5 px-3 py-1.5 text-sm text-gray-600 hover:text-red-600 rounded-md hover:bg-red-50 transition"
              >
                <LogOut size={16} />
                Logout
              </button>
            </>
          ) : (
            <Link
              to="/login"
              className="flex items-center gap-1.5 px-3 py-1.5 text-sm text-blue-600 hover:bg-blue-50 rounded-md transition"
            >
              <LogIn size={16} />
              Login
            </Link>
          )}
        </div>
      </div>
    </nav>
  )
}

function NavLink({ to, icon, label }) {
  return (
    <Link
      to={to}
      className="flex items-center gap-1.5 px-3 py-1.5 text-sm text-gray-600 hover:text-blue-600 hover:bg-blue-50 rounded-md transition"
    >
      {icon}
      {label}
    </Link>
  )
}
