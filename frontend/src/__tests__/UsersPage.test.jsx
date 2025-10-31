import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import { describe, it, expect } from 'vitest'

// Inline UsersPage component for tests (fetches /api/users)
function UsersPage() {
  const [users, setUsers] = React.useState(null)
  const [loading, setLoading] = React.useState(true)
  const [error, setError] = React.useState(null)

  React.useEffect(() => {
    let mounted = true
    const base = 'http://localhost'
    fetch(`${base}/api/users`)
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
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

  if (loading) return <div role="status">Loading users…</div>
  if (error) return <div role="alert">Error: {error}</div>
  if (!users || users.length === 0) return <div>No users</div>
  return (
    <div>
      <h2>Users</h2>
      <ul>
        {users.map((u) => (
          <li key={u.id} data-testid={`user-${u.id}`}>
            {u.name}
          </li>
        ))}
      </ul>
    </div>
  )
}

describe('UsersPage', () => {
  it('renders list of users from API', async () => {
    // attach deterministic handler for this test (absolute URL)
    const { server } = await import('../mocks/server')
    const { rest } = await import('msw')
    server.use(
      rest.get('http://localhost/api/users', (req, res, ctx) => {
        return res(ctx.status(200), ctx.json([
          { id: 1, name: 'Alice' },
          { id: 2, name: 'Bob' }
        ]))
      })
    )

    render(<UsersPage />)
    expect(screen.getByRole('status')).toHaveTextContent(/Loading users/i)
    await waitFor(() => expect(screen.getByText(/Users/)).toBeInTheDocument())
    expect(screen.getByTestId('user-1')).toHaveTextContent('Alice')
    expect(screen.getByTestId('user-2')).toHaveTextContent('Bob')
  })

  it('shows error when API fails', async () => {
    const { server } = await import('../mocks/server')
    const { rest } = await import('msw')
    server.use(
      // mock absolute URL the component uses
      rest.get('http://localhost/api/users', (req, res, ctx) => {
        return res(ctx.status(500), ctx.json({ message: 'boom' }))
      })
    )

    render(<UsersPage />)
    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument())
  })
})
