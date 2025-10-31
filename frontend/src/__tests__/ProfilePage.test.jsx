import React from 'react'
import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'

function ProfilePage() {
  const [profile, setProfile] = React.useState(null)
  const [loading, setLoading] = React.useState(true)
  const [error, setError] = React.useState(null)

  React.useEffect(() => {
    let mounted = true
    const base = 'http://localhost'
    fetch(`${base}/api/profile`)
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`)
        return r.json()
      })
      .then((data) => {
        if (mounted) setProfile(data)
      })
      .catch((e) => {
        if (mounted) setError(e.message)
      })
      .finally(() => {
        if (mounted) setLoading(false)
      })
    return () => (mounted = false)
  }, [])

  if (loading) return <div role="status">Loading profile…</div>
  if (error) return <div role="alert">Error: {error}</div>
  return (
    <div>
      <h2>Profile</h2>
      <div data-testid="profile-email">{profile.email}</div>
      <div data-testid="profile-name">{profile.name}</div>
    </div>
  )
}

describe('ProfilePage', () => {
  it('renders profile from API', async () => {
    const { server } = await import('../mocks/server')
    const { rest } = await import('msw')
    server.use(
      rest.get('http://localhost/api/profile', (req, res, ctx) => {
        return res(ctx.status(200), ctx.json({ id: 1, email: 'u@x.com', name: 'User X' }))
      })
    )

    render(<ProfilePage />)
    expect(screen.getByRole('status')).toBeInTheDocument()
    const email = await screen.findByTestId('profile-email')
    expect(email).toHaveTextContent('u@x.com')
    const name = screen.getByTestId('profile-name')
    expect(name).toHaveTextContent('User X')
  })

  it('shows error when profile API fails', async () => {
    const { server } = await import('../mocks/server')
    const { rest } = await import('msw')
    server.use(
      // mock absolute URL used by component
      rest.get('http://localhost/api/profile', (req, res, ctx) => res(ctx.status(500), ctx.json({ message: 'boom' })))
    )

    render(<ProfilePage />)
    const alert = await screen.findByRole('alert')
    expect(alert).toBeInTheDocument()
  })
})
