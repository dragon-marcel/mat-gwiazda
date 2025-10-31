import React from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect } from 'vitest'

// Inline useAuth hook: performs login against /api/auth/login
function useAuth() {
  const [token, setToken] = React.useState(null)
  const [error, setError] = React.useState(null)
  const [loading, setLoading] = React.useState(false)

  async function login(email, password) {
    setLoading(true)
    setError(null)
    try {
      const base = 'http://localhost'
      const res = await fetch(`${base}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      })
      const body = await res.json()
      if (!res.ok) throw new Error(body.message || 'error')
      setToken(body.token)
      return true
    } catch (e) {
      setError(e.message)
      return false
    } finally {
      setLoading(false)
    }
  }

  return { token, error, loading, login }
}

function AuthConsumer() {
  const { token, error, loading, login } = useAuth()
  const [email, setEmail] = React.useState('')
  const [password, setPassword] = React.useState('')

  return (
    <div>
      <form
        onSubmit={async (e) => {
          e.preventDefault()
          await login(email, password)
        }}
      >
        <input placeholder="email" value={email} onChange={(e) => setEmail(e.target.value)} />
        <input placeholder="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        <button type="submit">Login</button>
      </form>
      {loading && <div role="status">loading</div>}
      {token && <div role="status">token:{token}</div>}
      {error && <div role="alert">error:{error}</div>}
    </div>
  )
}

describe('useAuth hook', () => {
  it('logs in successfully with correct credentials', async () => {
    const { server } = await import('../mocks/server')
    const { rest } = await import('msw')
    server.use(
      rest.post('http://localhost/api/auth/login', (req, res, ctx) => {
        return res(ctx.status(200), ctx.json({ token: 'fake-jwt-token' }))
      })
    )

    render(<AuthConsumer />)
    const user = userEvent.setup()
    await user.type(screen.getByPlaceholderText('email'), 'user@example.com')
    await user.type(screen.getByPlaceholderText('password'), 'password')
    await user.click(screen.getByRole('button', { name: /login/i }))

    const status = await screen.findByRole('status')
    expect(status).toHaveTextContent(/token:fake-jwt-token/i)
  })

  it('shows error on bad credentials', async () => {
    const { server } = await import('../mocks/server')
    const { rest } = await import('msw')
    server.use(
      rest.post('http://localhost/api/auth/login', (req, res, ctx) => {
        return res(ctx.status(401), ctx.json({ message: 'Invalid credentials' }))
      })
    )

    render(<AuthConsumer />)
    const user = userEvent.setup()
    await user.type(screen.getByPlaceholderText('email'), 'bad@example.com')
    await user.type(screen.getByPlaceholderText('password'), 'wrong')
    await user.click(screen.getByRole('button', { name: /login/i }))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent(/error/i)
  })
})

