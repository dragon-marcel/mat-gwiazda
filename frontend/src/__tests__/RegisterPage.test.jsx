import React from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect } from 'vitest'

function RegisterPage() {
  const [message, setMessage] = React.useState(null)
  async function onSubmit(e) {
    e.preventDefault()
    const form = new FormData(e.target)
    const email = form.get('email')
    const password = form.get('password')
    try {
      const res = await fetch('http://localhost/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      })
      const body = await res.json()
      if (!res.ok) throw new Error(body.message || 'error')
      setMessage(`registered:${body.email}`)
    } catch (e) {
      setMessage(`error:${e.message}`)
    }
  }

  return (
    <form onSubmit={onSubmit} aria-label="register-page">
      <input name="email" placeholder="email" />
      <input name="password" placeholder="password" />
      <button type="submit">Register</button>
      {message && <div role="status">{message}</div>}
    </form>
  )
}

describe('RegisterPage', () => {
  it('registers successfully', async () => {
    const { server } = await import('../mocks/server')
    const { rest } = await import('msw')
    server.use(
      rest.post('http://localhost/api/auth/register', (req, res, ctx) => {
        return res(ctx.status(201), ctx.json({ id: 10, email: 'new@x.com' }))
      })
    )

    render(<RegisterPage />)
    const user = userEvent.setup()
    await user.type(screen.getByPlaceholderText('email'), 'new@x.com')
    await user.type(screen.getByPlaceholderText('password'), 'strongpass')
    await user.click(screen.getByRole('button', { name: /register/i }))

    const status = await screen.findByRole('status')
    expect(status).toHaveTextContent(/registered:new@x.com/)
  })

  it('shows validation error for weak password', async () => {
    render(<RegisterPage />)
    const user = userEvent.setup()
    await user.type(screen.getByPlaceholderText('email'), 'bad@x.com')
    await user.type(screen.getByPlaceholderText('password'), '123')
    await user.click(screen.getByRole('button', { name: /register/i }))

    const status = await screen.findByRole('status')
    expect(status).toHaveTextContent(/error/i)
  })
})

