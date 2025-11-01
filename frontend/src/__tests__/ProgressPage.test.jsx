import React from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect } from 'vitest'

// Inline ProgressPage: sends progress to /api/progress
function ProgressPage() {
  const [status, setStatus] = React.useState(null)
  const [saving, setSaving] = React.useState(false)

  async function saveProgress() {
    setSaving(true)
    try {
      const base = 'http://localhost'
      const res = await fetch(`${base}/api/progress`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId: 1, progress: 42 }),
      })
      const body = await res.json()
      if (!res.ok) throw new Error(body.message || 'error')
      setStatus(`saved:${body.ok}`)
    } catch (e) {
      setStatus(`error:${e.message}`)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div>
      <h2>Progress</h2>
      <button onClick={saveProgress} disabled={saving}>Save progress</button>
      {status && <div role="status">{status}</div>}
    </div>
  )
}

describe('ProgressPage', () => {
  it('saves progress successfully', async () => {
    render(<ProgressPage />)
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: /save progress/i }))
    const status = await screen.findByRole('status')
    expect(status).toHaveTextContent(/saved:true/i)
  })

  it('shows error when save fails', async () => {
    const { server } = await import('../mocks/server')
    const { rest } = await import('msw')
    server.use(
      // mock absolute URL used by component
      rest.post('http://localhost/api/progress', (req, res, ctx) => {
        return res(ctx.status(500), ctx.json({ message: 'boom' }))
      })
    )

    render(<ProgressPage />)
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: /save progress/i }))
    const status = await screen.findByRole('status')
    expect(status).toHaveTextContent(/error/i)
  })
})
