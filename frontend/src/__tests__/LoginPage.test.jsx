import React from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect } from 'vitest'

function LoginPage() {
  const [message, setMessage] = React.useState(null)
  async function onSubmit(e) {
    e.preventDefault()
    const form = new FormData(e.target)
    const email = form.get('email')
    const password = form.get('password')
    try {
      const res = await fetch('http://localhost/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      })
      const body = await res.json()
      if (!res.ok) throw new Error(body.message || 'error')
      setMessage(`token:${body.token}`)
    } catch (e) {
      setMessage(`error:${e.message}`)
    }
  }

  return (
    <form onSubmit={onSubmit} aria-label="login-page">
      <input name="email" placeholder="email" />
      <input name="password" placeholder="password" />
      <button type="submit">Login</button>
      {message && <div role="status">{message}</div>}
    </form>
  )
}

describe('LoginPage', () => {
  it('logs in successfully and shows token', async () => {
    const { server } = await import('../mocks/server')
    const { rest } = await import('msw')
    server.use(
      rest.post('http://localhost/api/auth/login', (req, res, ctx) => {
        return res(ctx.status(200), ctx.json({ token: 'abc-123' }))
      })
    )

    render(<LoginPage />)
    const user = userEvent.setup()
    await user.type(screen.getByPlaceholderText('email'), 'user@example.com')
    await user.type(screen.getByPlaceholderText('password'), 'password')
    await user.click(screen.getByRole('button', { name: /login/i }))

    const status = await screen.findByRole('status')
    expect(status).toHaveTextContent(/token:abc-123/)
  })

  it('shows error on bad credentials', async () => {
    const { server } = await import('../mocks/server')
    const { rest } = await import('msw')
    server.use(
      rest.post('http://localhost/api/auth/login', (req, res, ctx) => {
        return res(ctx.status(401), ctx.json({ message: 'Invalid' }))
      })
    )

    render(<LoginPage />)
    const user = userEvent.setup()
    await user.type(screen.getByPlaceholderText('email'), 'bad')
    await user.type(screen.getByPlaceholderText('password'), 'wrong')
    await user.click(screen.getByRole('button', { name: /login/i }))

    const status = await screen.findByRole('status')
    expect(status).toHaveTextContent(/error/i)
  })
})

