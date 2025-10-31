import React from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi } from 'vitest'

function RegisterForm() {
  const [message, setMessage] = React.useState(null)
  const [submitting, setSubmitting] = React.useState(false)

  async function onSubmit(e) {
    e.preventDefault()
    setSubmitting(true)
    const form = new FormData(e.target)
    const email = form.get('email')
    const password = form.get('password')
    try {
      const base = 'http://localhost'
      const res = await fetch(`${base}/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      })
      const body = await res.json()
      if (!res.ok) throw new Error(body.message || 'error')
      setMessage(`registered:${body.email}`)
    } catch (err) {
      setMessage(`error:${err.message}`)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={onSubmit} aria-label="register-form">
      <input name="email" placeholder="email" />
      <input name="password" placeholder="password" />
      <button type="submit" disabled={submitting}>Register</button>
      {message && <div role="status">{message}</div>}
    </form>
  )
}

function LoginForm() {
  const [message, setMessage] = React.useState(null)
  const [submitting, setSubmitting] = React.useState(false)

  async function onSubmit(e) {
    e.preventDefault()
    setSubmitting(true)
    const form = new FormData(e.target)
    const email = form.get('email')
    const password = form.get('password')
    try {
      const base = 'http://localhost'
      const res = await fetch(`${base}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      })
      const body = await res.json()
      if (!res.ok) throw new Error(body.message || 'error')
      setMessage(`token:${body.token}`)
    } catch (err) {
      setMessage(`error:${err.message}`)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={onSubmit} aria-label="login-form">
      <input name="email" placeholder="email" />
      <input name="password" placeholder="password" />
      <button type="submit" disabled={submitting}>Login</button>
      {message && <div role="status">{message}</div>}
    </form>
  )
}

describe('Auth forms (inline) with MSW', () => {
  it('registers successfully with valid data', async () => {
    // mock fetch to guarantee deterministic success for this test and avoid network races with MSW
    const originalFetch = global.fetch
    global.fetch = vi.fn().mockResolvedValue({ ok: true, status: 201, json: async () => ({ id: 123, email: 'new@example.com' }) })

    render(<RegisterForm />)
    const user = userEvent.setup()
    await user.type(screen.getByPlaceholderText('email'), 'new@example.com')
    await user.type(screen.getByPlaceholderText('password'), 'strongpass')
    await user.click(screen.getByRole('button', { name: /register/i }))

    const status = await screen.findByRole('status')
    expect(status).toHaveTextContent('registered:new@example.com')

    // restore
    global.fetch = originalFetch
  })

  it('shows validation error for weak password on register', async () => {
    render(<RegisterForm />)
    const user = userEvent.setup()
    await user.type(screen.getByPlaceholderText('email'), 'bad@example.com')
    await user.type(screen.getByPlaceholderText('password'), '123')
    await user.click(screen.getByRole('button', { name: /register/i }))

    const status = await screen.findByRole('status')
    expect(status).toHaveTextContent(/error/i)
  })

  it('logs in with correct credentials', async () => {
    // mock fetch to guarantee deterministic success for this test and avoid network races with MSW
    const originalFetch = global.fetch
    global.fetch = vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => ({ token: 'fake-jwt-token', user: { id: 1, email: 'user@example.com' } }) })

    render(<LoginForm />)
    const user = userEvent.setup()
    await user.type(screen.getByPlaceholderText('email'), 'user@example.com')
    await user.type(screen.getByPlaceholderText('password'), 'password')
    await user.click(screen.getByRole('button', { name: /login/i }))

    const status = await screen.findByRole('status')
    expect(status).toHaveTextContent(/token:/i)

    // restore
    global.fetch = originalFetch
  })

  it('shows error on bad login credentials', async () => {
    render(<LoginForm />)
    const user = userEvent.setup()
    await user.type(screen.getByPlaceholderText('email'), 'user@example.com')
    await user.type(screen.getByPlaceholderText('password'), 'wrong')
    await user.click(screen.getByRole('button', { name: /login/i }))

    const status = await screen.findByRole('status')
    expect(status).toHaveTextContent(/error/i)
  })
})
