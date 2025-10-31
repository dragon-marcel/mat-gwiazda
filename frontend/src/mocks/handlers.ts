import { rest } from 'msw'

// Primary handlers (path-based)
export const handlers = [
  // GET tasks
  rest.get('/api/tasks', (req, res, ctx) => {
    // eslint-disable-next-line no-console
    console.log('[msw] default handler: /api/tasks')
    return res(
      ctx.status(200),
      ctx.json([
        { id: 1, question: '2+2=?', options: ['3', '4', '5'] },
        { id: 2, question: 'Capital of Poland?', options: ['Berlin', 'Warsaw'] },
      ])
    )
  }),

  // Absolute URL variant for tasks
  rest.get(new RegExp('https?://.*\\/api\\/tasks$'), (req, res, ctx) => {
    // eslint-disable-next-line no-console
    console.log('[msw] default handler: GET absolute /api/tasks')
    return res(
      ctx.status(200),
      ctx.json([
        { id: 1, question: '2+2=?', options: ['3', '4', '5'] },
        { id: 2, question: 'Capital of Poland?', options: ['Berlin', 'Warsaw'] },
      ])
    )
  }),

  // error example
  rest.get('/api/tasks-error', (req, res, ctx) => {
    // eslint-disable-next-line no-console
    console.log('[msw] default handler: /api/tasks-error')
    return res(ctx.status(500), ctx.json({ message: 'Internal Server Error' }))
  }),

  // Auth: register (path)
  rest.post('/api/auth/register', async (req, res, ctx) => {
    // eslint-disable-next-line no-console
    console.log('[msw] default handler: POST /api/auth/register')
    const body = await req.json()
    const { email, password } = body || {}
    if (!email || !password || password.length < 6) {
      return res(ctx.status(400), ctx.json({ message: 'Invalid data' }))
    }
    return res(ctx.status(201), ctx.json({ id: 123, email }))
  }),

  // Absolute variant for auth/register
  rest.post(new RegExp('https?://.*\\/api\\/auth\\/register$'), async (req, res, ctx) => {
    // eslint-disable-next-line no-console
    console.log('[msw] default handler: POST absolute /api/auth/register')
    const body = await req.json()
    const { email, password } = body || {}
    if (!email || !password || password.length < 6) {
      return res(ctx.status(400), ctx.json({ message: 'Invalid data' }))
    }
    return res(ctx.status(201), ctx.json({ id: 123, email }))
  }),

  // Auth: login (path)
  rest.post('/api/auth/login', async (req, res, ctx) => {
    // eslint-disable-next-line no-console
    console.log('[msw] default handler: POST /api/auth/login')
    const body = await req.json()
    const { email, password } = body || {}
    if (email === 'user@example.com' && password === 'password') {
      return res(ctx.status(200), ctx.json({ token: 'fake-jwt-token', user: { id: 1, email } }))
    }
    return res(ctx.status(401), ctx.json({ message: 'Invalid credentials' }))
  }),

  // Absolute variant for auth/login
  rest.post(new RegExp('https?://.*\\/api\\/auth\\/login$'), async (req, res, ctx) => {
    // eslint-disable-next-line no-console
    console.log('[msw] default handler: POST absolute /api/auth/login')
    const body = await req.json()
    const { email, password } = body || {}
    if (email === 'user@example.com' && password === 'password') {
      return res(ctx.status(200), ctx.json({ token: 'fake-jwt-token', user: { id: 1, email } }))
    }
    return res(ctx.status(401), ctx.json({ message: 'Invalid credentials' }))
  }),

  // Users endpoint
  rest.get('/api/users', (req, res, ctx) => {
    // eslint-disable-next-line no-console
    console.log('[msw] default handler: /api/users')
    return res(ctx.status(200), ctx.json([
      { id: 1, name: 'Alice' },
      { id: 2, name: 'Bob' }
    ]))
  }),

  // Absolute variant for users
  rest.get(new RegExp('https?://.*\\/api\\/users$'), (req, res, ctx) => {
    // eslint-disable-next-line no-console
    console.log('[msw] default handler: GET absolute /api/users')
    return res(ctx.status(200), ctx.json([
      { id: 1, name: 'Alice' },
      { id: 2, name: 'Bob' }
    ]))
  }),

  // Progress endpoint - accept POST to save progress
  rest.post('/api/progress', async (req, res, ctx) => {
    // eslint-disable-next-line no-console
    console.log('[msw] default handler: POST /api/progress')
    const body = await req.json()
    // echo back for test verification
    return res(ctx.status(200), ctx.json({ ok: true, received: body }))
  }),

  // Absolute variant for progress
  rest.post(new RegExp('https?://.*\\/api\\/progress$'), async (req, res, ctx) => {
    // eslint-disable-next-line no-console
    console.log('[msw] default handler: POST absolute /api/progress')
    const body = await req.json()
    return res(ctx.status(200), ctx.json({ ok: true, received: body }))
  }),

  // Profile endpoint
  rest.get('/api/profile', (req, res, ctx) => {
    // eslint-disable-next-line no-console
    console.log('[msw] default handler: /api/profile')
    return res(ctx.status(200), ctx.json({ id: 1, email: 'user@example.com', name: 'User One' }))
  }),

  // Absolute variant for profile
  rest.get(new RegExp('https?://.*\\/api\\/profile$'), (req, res, ctx) => {
    // eslint-disable-next-line no-console
    console.log('[msw] default handler: GET absolute /api/profile')
    return res(ctx.status(200), ctx.json({ id: 1, email: 'user@example.com', name: 'User One' }))
  }),

  // Admin: list users
  rest.get('/api/admin/users', (req, res, ctx) => {
    // eslint-disable-next-line no-console
    console.log('[msw] default handler: /api/admin/users')
    return res(ctx.status(200), ctx.json([
      { id: 1, email: 'a@x.com', role: 'user' },
      { id: 2, email: 'b@x.com', role: 'admin' }
    ]))
  }),

  // Absolute variant for admin users
  rest.get(new RegExp('https?://.*\\/api\\/admin\\/users$'), (req, res, ctx) => {
    // eslint-disable-next-line no-console
    console.log('[msw] default handler: GET absolute /api/admin/users')
    return res(ctx.status(200), ctx.json([
      { id: 1, email: 'a@x.com', role: 'user' },
      { id: 2, email: 'b@x.com', role: 'admin' }
    ]))
  }),

  // Task detail
  rest.get('/api/task/:id', (req, res, ctx) => {
    // eslint-disable-next-line no-console
    console.log('[msw] default handler: /api/task/:id', req.params)
    const { id } = req.params
    return res(ctx.status(200), ctx.json({ id, question: `Question ${id}`, options: ['A', 'B'] }))
  }),

  // Absolute variant for task detail
  rest.get(new RegExp('https?://.*\\/api\\/task\\/\\d+$'), (req, res, ctx) => {
    // eslint-disable-next-line no-console
    console.log('[msw] default handler: GET absolute /api/task/:id', req.params)
    const path = req.url.pathname || ''
    const id = (path.match(/\/api\/task\/(\d+)$/) || [])[1]
    return res(ctx.status(200), ctx.json({ id, question: `Question ${id}`, options: ['A', 'B'] }))
  }),

  // Submit answer
  rest.post('/api/task/:id/submit', async (req, res, ctx) => {
    // eslint-disable-next-line no-console
    console.log('[msw] default handler: POST /api/task/:id/submit', req.params)
    const { id } = req.params
    const body = await req.json()
    return res(ctx.status(200), ctx.json({ id, correct: body.answer === 'A' }))
  }),

  // Absolute variant for submit
  rest.post(new RegExp('https?://.*\\/api\\/task\\/\\d+\\/submit$'), async (req, res, ctx) => {
    // eslint-disable-next-line no-console
    console.log('[msw] default handler: POST absolute /api/task/:id/submit', req.params)
    const path = req.url.pathname || ''
    const id = (path.match(/\/api\/task\/(\d+)\/submit$/) || [])[1]
    const body = await req.json()
    return res(ctx.status(200), ctx.json({ id, correct: body.answer === 'A' }))
  }),
]


export default handlers
