import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import { describe, it, expect } from 'vitest'

// Inline hook: useTasks
function useTasks() {
  const [tasks, setTasks] = React.useState(null)
  const [loading, setLoading] = React.useState(true)
  const [error, setError] = React.useState(null)

  React.useEffect(() => {
    let mounted = true
    const base = 'http://localhost'
    fetch(`${base}/api/tasks`)
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`)
        return r.json()
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

  return { tasks, loading, error }
}

// Test harness component
function TasksConsumer() {
  const { tasks, loading, error } = useTasks()
  if (loading) return <div role="status">loading</div>
  if (error) return <div role="alert">Error: {error}</div>
  return (
    <ul>
      {tasks.map((t) => (
        <li key={t.id} data-testid={`task-${t.id}`}>{t.question}</li>
      ))}
    </ul>
  )
}

describe('useTasks hook', () => {
  it('fetches and displays tasks', async () => {
    render(<TasksConsumer />)
    expect(screen.getByRole('status')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText(/2\+2=\?/)).toBeInTheDocument())
    expect(screen.getByTestId('task-1')).toHaveTextContent('2+2=?')
  })

  it('shows error when API fails', async () => {
    const { server } = await import('../mocks/server')
    const { rest } = await import('msw')
    server.use(
      // mock absolute URL used by component
      rest.get('http://localhost/api/tasks', (req, res, ctx) => res(ctx.status(500), ctx.json({ message: 'boom' })))
    )

    render(<TasksConsumer />)
    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument())
  })
})
