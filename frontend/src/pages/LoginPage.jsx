import { useState } from 'react'
import { api } from '../api/client'

export default function LoginPage({ onLogin }) {
  const [mode, setMode] = useState('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState('BORROWER')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      if (mode === 'register') {
        await api.post('/auth/register', { email, password, role })
      }
      const auth = await api.post('/auth/login', { email, password })
      const me = await api.get('/auth/me', auth.token)
      onLogin(auth.token, me.role)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <form onSubmit={handleSubmit} className="w-full max-w-sm bg-white p-6 rounded-lg shadow space-y-4">
        <h1 className="text-xl font-semibold text-gray-900">Microloan Platform</h1>

        <div className="flex gap-2 text-sm">
          <button
            type="button"
            onClick={() => setMode('login')}
            className={`flex-1 py-1.5 rounded cursor-pointer ${mode === 'login' ? 'bg-gray-900 text-white' : 'bg-gray-100 text-gray-700'}`}
          >
            Log in
          </button>
          <button
            type="button"
            onClick={() => setMode('register')}
            className={`flex-1 py-1.5 rounded cursor-pointer ${mode === 'register' ? 'bg-gray-900 text-white' : 'bg-gray-100 text-gray-700'}`}
          >
            Register
          </button>
        </div>

        <div>
          <label className="block text-sm text-gray-600 mb-1">Email</label>
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full border border-gray-300 rounded px-3 py-1.5"
          />
        </div>

        <div>
          <label className="block text-sm text-gray-600 mb-1">Password</label>
          <input
            type="password"
            required
            minLength={8}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full border border-gray-300 rounded px-3 py-1.5"
          />
        </div>

        {mode === 'register' && (
          <div>
            <label className="block text-sm text-gray-600 mb-1">Role</label>
            <select
              value={role}
              onChange={(e) => setRole(e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-1.5"
            >
              <option value="BORROWER">Borrower</option>
              <option value="ADMIN">Admin</option>
            </select>
          </div>
        )}

        {error && <p className="text-sm text-red-600">{error}</p>}

        <button
          type="submit"
          disabled={busy}
          className="w-full bg-gray-900 text-white rounded py-2 disabled:opacity-50 cursor-pointer"
        >
          {busy ? 'Please wait...' : mode === 'login' ? 'Log in' : 'Register & log in'}
        </button>
      </form>
    </div>
  )
}
