import { useState } from 'react'
import LoginPage from './pages/LoginPage'
import BorrowerPage from './pages/BorrowerPage'
import AdminPage from './pages/AdminPage'

const STORAGE_KEY = 'microloan.auth'

function loadAuth() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export default function App() {
  const [auth, setAuth] = useState(loadAuth)

  function handleLogin(token, role) {
    const next = { token, role }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    setAuth(next)
  }

  function handleLogout() {
    localStorage.removeItem(STORAGE_KEY)
    setAuth(null)
  }

  if (!auth) {
    return <LoginPage onLogin={handleLogin} />
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b border-gray-200 px-6 py-3 flex items-center justify-between">
        <h1 className="font-semibold text-gray-900">Microloan Platform</h1>
        <div className="flex items-center gap-3 text-sm text-gray-600">
          <span className="px-2 py-0.5 rounded bg-gray-100">{auth.role}</span>
          <button type="button" onClick={handleLogout} className="text-gray-500 hover:text-gray-900 underline cursor-pointer">
            Log out
          </button>
        </div>
      </header>

      <main className="p-6 max-w-4xl mx-auto">
        {auth.role === 'ADMIN' ? <AdminPage token={auth.token} /> : <BorrowerPage token={auth.token} />}
      </main>
    </div>
  )
}
