import React from 'react'
import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'

function AdminPage() {
  const [users, setUsers] = React.useState(null)
  const [loading, setLoading] = React.useState(true)
  const [error, setError] = React.useState(null)

  React.useEffect(() => {
    let mounted = true
    const base = 'http://localhost'
    fetch(`${base}/api/admin/users`)
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`)
        return r.json()
      })
      .then((data) => {
        if (mounted) setUsers(data)
      })
      .catch((e) => {
        if (mounted) setError(e.message)
      })
      .finally(() => {
        if (mounted) setLoading(false)
      })
    return () => (mounted = false)
  }, [])

  if (loading) return <div role="status">Loading admin users…</div>
  if (error) return <div role="alert">Error: {error}</div>
  return (
    <div>
      <h2>Admin users</h2>
      <ul>
        {users.map((u) => (
          <li key={u.id} data-testid={`admin-user-${u.id}`}>
            {u.email} - {u.role}
          </li>
        ))}
      </ul>
    </div>
  )
}

describe('AdminPage', () => {
  it('renders admin users list', async () => {
    const { server } = await import('../mocks/server')
    const { rest } = await import('msw')
    server.use(
      rest.get('http://localhost/api/admin/users', (req, res, ctx) => {
        return res(ctx.status(200), ctx.json([
          { id: 1, email: 'a@x.com', role: 'user' },
          { id: 2, email: 'b@x.com', role: 'admin' }
        ]))
      })
    )

    render(<AdminPage />)
    expect(screen.getByRole('status')).toBeInTheDocument()
    const item1 = await screen.findByTestId('admin-user-1')
    expect(item1).toHaveTextContent('a@x.com')
    const item2 = screen.getByTestId('admin-user-2')
    expect(item2).toHaveTextContent('admin')
  })

  it('shows error when admin API fails', async () => {
    const { server } = await import('../mocks/server')
    const { rest } = await import('msw')
    server.use(
      // Mock the exact absolute URL the component calls so MSW intercepts the request
      rest.get('http://localhost/api/admin/users', (req, res, ctx) =>
        res(ctx.status(500), ctx.json({ message: 'boom' }))
      )
    )

    render(<AdminPage />)
    const alert = await screen.findByRole('alert')
    expect(alert).toBeInTheDocument()
  })
})
