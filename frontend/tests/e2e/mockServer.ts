import { Page } from '@playwright/test'

function tryParsePostData(routeRequest: any) {
  try {
    const pd = routeRequest.postData()
    if (!pd) return null
    return JSON.parse(pd)
  } catch (e) {
    return null
  }
}

export async function setupMockApi(page: Page) {
  // tasks
  await page.route('**/api/tasks', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { id: 1, question: '2+2=?', options: ['3', '4', '5'] },
        { id: 2, question: 'Capital of Poland?', options: ['Berlin', 'Warsaw'] },
      ]),
    })
  })

  // profile
  await page.route('**/api/profile', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ id: 1, email: 'user@example.com', name: 'User One' }),
    })
  })

  // users
  await page.route('**/api/users', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { id: 1, name: 'Alice' },
        { id: 2, name: 'Bob' },
      ]),
    })
  })

  // admin users
  await page.route('**/api/admin/users', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { id: 1, email: 'a@x.com', role: 'user' },
        { id: 2, email: 'b@x.com', role: 'admin' },
      ]),
    })
  })

  // admin change role (new handler)
  await page.route('**/api/admin/users/*/role', async (route) => {
    const body = tryParsePostData(route.request()) || {}
    const url = route.request().url()
    const match = url.match(/\/api\/admin\/users\/(\d+)\/role/)
    const id = match ? Number(match[1]) : null
    const newRole = body && body.role ? body.role : 'user'
    const email = id ? `user${id}@x.com` : 'unknown@x.com'
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ id, email, role: newRole }),
    })
  })

  // admin delete user (NEW)
  await page.route('**/api/admin/users/*', async (route) => {
    const url = route.request().url()
    const method = route.request().method()
    const match = url.match(/\/api\/admin\/users\/(\d+)$/)
    const id = match ? Number(match[1]) : null
    if (method.toUpperCase() === 'DELETE') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ id, deleted: true }),
      })
      return
    }
    // fallback: if not DELETE, pass through to admin users list
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { id: 1, email: 'a@x.com', role: 'user' },
        { id: 2, email: 'b@x.com', role: 'admin' },
      ]),
    })
  })

  // progress POST
  await page.route('**/api/progress', async (route) => {
    // echo back a successful response
    const post = tryParsePostData(route.request())
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ ok: true, received: post }),
    })
  })

  // auth login
  await page.route('**/api/auth/login', async (route) => {
    const body = tryParsePostData(route.request()) || {}
    const { email, password } = body || {}
    // Accept any email as long as password equals 'password' to support registered test users
    if (password === 'password') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ token: 'fake-jwt-token', user: { id: 1, email } }),
      })
    } else {
      await route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify({ message: 'Invalid credentials' }) })
    }
  })

  // auth register
  await page.route('**/api/auth/register', async (route) => {
    const body = tryParsePostData(route.request()) || {}
    const { email, password } = body || {}
    if (!email || !password || String(password).length < 6) {
      await route.fulfill({ status: 400, contentType: 'application/json', body: JSON.stringify({ message: 'Invalid data' }) })
      return
    }
    await route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify({ id: 123, email }) })
  })

  // task detail
  await page.route('**/api/task/*', async (route) => {
    const url = route.request().url()
    const match = url.match(/\/api\/task\/(\d+)/)
    const id = match ? Number(match[1]) : 1
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ id, question: `Question ${id}`, options: ['A', 'B'] }),
    })
  })

  // submit answer
  await page.route('**/api/task/*/submit', async (route) => {
    const body = tryParsePostData(route.request()) || {}
    const url = route.request().url()
    const match = url.match(/\/api\/task\/(\d+)\/submit/)
    const id = match ? Number(match[1]) : 1
    const correct = body && body.answer === 'A'
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ id, correct }),
    })
  })

  // Prompts CRUD (new handlers)
  // Simple in-memory prompts store per test-run
  const promptsStore = [
    { id: 1, title: 'Sample prompt 1', body: 'Do X' },
    { id: 2, title: 'Sample prompt 2', body: 'Do Y' },
  ]

  // GET /api/prompts and POST /api/prompts (create)
  await page.route('**/api/prompts', async (route) => {
    const method = (route.request().method() || 'GET').toUpperCase()
    if (method === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(promptsStore),
      })
      return
    }
    if (method === 'POST') {
      const body = tryParsePostData(route.request()) || {}
      const nextId = promptsStore.length ? Math.max(...promptsStore.map(p => p.id)) + 1 : 1
      const created = { id: nextId, title: body.title || `Prompt ${nextId}`, body: body.body || '' }
      promptsStore.push(created)
      await route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(created) })
      return
    }
    // fallback: return list
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(promptsStore) })
  })

  // GET /api/prompts/:id
  await page.route('**/api/prompts/*', async (route) => {
    const url = route.request().url()
    const match = url.match(/\/api\/prompts\/(\d+)$/)
    if (route.request().method().toUpperCase() === 'GET' && match) {
      const id = Number(match[1])
      const item = promptsStore.find((p) => p.id === id)
      if (item) {
        await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(item) })
      } else {
        await route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify({ message: 'Not found' }) })
      }
      return
    }

    // PUT /api/prompts/:id => update
    if (route.request().method().toUpperCase() === 'PUT' && match) {
      const id = Number(match[1])
      const body = tryParsePostData(route.request()) || {}
      const idx = promptsStore.findIndex((p) => p.id === id)
      if (idx >= 0) {
        promptsStore[idx] = { ...promptsStore[idx], ...body }
        await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(promptsStore[idx]) })
      } else {
        await route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify({ message: 'Not found' }) })
      }
      return
    }

    // DELETE /api/prompts/:id
    if (route.request().method().toUpperCase() === 'DELETE' && match) {
      const id = Number(match[1])
      const idx = promptsStore.findIndex((p) => p.id === id)
      if (idx >= 0) {
        promptsStore.splice(idx, 1)
        await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ id, deleted: true }) })
      } else {
        await route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify({ message: 'Not found' }) })
      }
      return
    }

    // default fallback: return list
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(promptsStore) })
  })
}
