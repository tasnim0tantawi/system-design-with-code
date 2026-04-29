import { createContext, useContext, useState } from 'react'

const Ctx = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const token = localStorage.getItem('token')
    const userId = localStorage.getItem('userId')
    const role = localStorage.getItem('role')
    return token ? { token, userId: Number(userId), role } : null
  })

  function login(tokenResponse) {
    localStorage.setItem('token', tokenResponse.token)
    localStorage.setItem('userId', String(tokenResponse.userId))
    localStorage.setItem('role', tokenResponse.role)
    setUser({ token: tokenResponse.token, userId: tokenResponse.userId, role: tokenResponse.role })
  }

  function logout() {
    localStorage.clear()
    setUser(null)
  }

  return <Ctx.Provider value={{ user, login, logout }}>{children}</Ctx.Provider>
}

export const useAuth = () => useContext(Ctx)
