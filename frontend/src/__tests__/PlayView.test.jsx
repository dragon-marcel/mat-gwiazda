import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import { describe, it, expect } from 'vitest'

function PlayView() {
  const [tasks, setTasks] = React.useState(null)
  const [loading, setLoading] = React.useState(true)
  const [error, setError] = React.useState(null)

  React.useEffect(() => {
    let mounted = true
    const base = (typeof location !== 'undefined' && location?.origin) ? location.origin : 'http://localhost'
    fetch(`${base}/api/tasks`)
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then((data) => {
        if (mounted) setTasks(data)
      })
      .catch((e) => {
        if (mounted) setError(e.message)
      })
      .finally(() => {
        if (mounted) setLoading(false)
      })
    return () => (mounted = false)
  }, [])

  if (loading) return <div role="status">Loading tasks…</div>
  if (error) return <div role="alert">Error: {error}</div>
  if (!tasks || tasks.length === 0) return <div>No tasks</div>
  return (
    <div>
      <h2>Tasks</h2>
      <ul>
        {tasks.map((t) => (
          <li key={t.id} data-testid={`task-${t.id}`}>
            <strong>{t.question}</strong>
          </li>
        ))}
      </ul>
    </div>
  )
}

describe('PlayView (inline) with MSW', () => {
  it('renders tasks from MSW handler', async () => {
    render(<PlayView />)
    expect(screen.getByRole('status')).toHaveTextContent(/Loading tasks/i)
    await waitFor(() => expect(screen.getByText(/Tasks/)).toBeInTheDocument())
    expect(screen.getByTestId('task-1')).toHaveTextContent('2+2=?')
  })

  it('shows error when server returns 500', async () => {
    // override handler to return 500 for this test
    const { server } = await import('../mocks/server')
    const { rest } = await import('msw')
    server.use(
      rest.get('/api/tasks', (req, res, ctx) => {
        return res(ctx.status(500), ctx.json({ message: 'boom' }))
      })
    )

    render(<PlayView />)
    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument())
    expect(screen.getByRole('alert')).toHaveTextContent(/Error/i)
  })
})
