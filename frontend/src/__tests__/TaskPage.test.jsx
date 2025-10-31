import React from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect } from 'vitest'

// Inline TaskPage: fetches task details and submits an answer
function TaskPage({ id = 1 }) {
  const [task, setTask] = React.useState(null)
  const [status, setStatus] = React.useState(null)

  React.useEffect(() => {
    let mounted = true
    const base = 'http://localhost'
    fetch(`${base}/api/task/${id}`)
      .then((r) => r.json())
      .then((data) => { if (mounted) setTask(data) })
      .catch((e) => { if (mounted) setStatus(`error:${e.message}`) })
    return () => (mounted = false)
  }, [id])

  async function submit(answer) {
    const base = 'http://localhost'
    const res = await fetch(`${base}/api/task/${id}/submit`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ answer }),
    })
    const body = await res.json()
    setStatus(body.correct ? 'correct' : 'incorrect')
  }

  if (!task) return <div role="status">Loading task…</div>
  return (
    <div>
      <h2>{task.question}</h2>
      <div>
        {task.options.map((o, i) => (
          <button key={i} onClick={() => submit(o)} data-testid={`opt-${i}`}>{o}</button>
        ))}
      </div>
      {status && <div role="status">{status}</div>}
    </div>
  )
}

describe('TaskPage', () => {
  it('renders task and submits correct answer', async () => {
    const { server } = await import('../mocks/server')
    const { rest } = await import('msw')
    server.use(
      rest.get('http://localhost/api/task/1', (req, res, ctx) => res(ctx.status(200), ctx.json({ id: 1, question: 'Q1', options: ['A','B'] }))),
      rest.post('http://localhost/api/task/1/submit', async (req, res, ctx) => {
        const body = await req.json()
        return res(ctx.status(200), ctx.json({ id: 1, correct: body.answer === 'A' }))
      })
    )

    render(<TaskPage id={1} />)
    const user = userEvent.setup()
    await user.click(await screen.findByTestId('opt-0'))
    const status = await screen.findByRole('status')
    expect(status).toHaveTextContent('correct')
  })

  it('shows incorrect for wrong answer', async () => {
    const { server } = await import('../mocks/server')
    const { rest } = await import('msw')
    server.use(
      rest.get('http://localhost/api/task/2', (req, res, ctx) => res(ctx.status(200), ctx.json({ id: 2, question: 'Q2', options: ['X','Y'] }))),
      rest.post('http://localhost/api/task/2/submit', async (req, res, ctx) => {
        const body = await req.json()
        return res(ctx.status(200), ctx.json({ id: 2, correct: body.answer === 'Z' }))
      })
    )

    render(<TaskPage id={2} />)
    const user = userEvent.setup()
    await user.click(await screen.findByTestId('opt-0'))
    const status = await screen.findByRole('status')
    expect(status).toHaveTextContent('incorrect')
  })
})
