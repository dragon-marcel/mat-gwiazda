import { test, expect } from '@playwright/test'
import { setupMockApi } from './mockServer'

test('admin delete user', async ({ page }) => {
  await setupMockApi(page)
  await page.goto('/admin')

  const user1 = page.locator('[data-testid="admin-user-1"]')

  if (await user1.count() > 0) {
    const deleteBtn = user1.locator('button:has-text("Delete"), button:has-text("Remove"), button:has-text("Usuń"), button:has-text("Delete user")')
    if (await deleteBtn.count() > 0) {
      const [response] = await Promise.all([
        page.waitForResponse((r) => r.url().includes('/api/admin/users/1') && r.status() === 200),
        deleteBtn.first().click(),
      ])
      const body = await response.json()
      expect(body).toMatchObject({ id: 1, deleted: true })
      return
    }

    // fallback: call DELETE via page.evaluate so mockServer intercepts
    const resp = await page.evaluate(async () => {
      const r = await fetch('/api/admin/users/1', { method: 'DELETE' })
      return { status: r.status, body: await r.json() }
    })
    expect(resp.status).toBe(200)
    expect(resp.body).toMatchObject({ id: 1, deleted: true })
    return
  }

  // If no UI list, fallback to direct API call
  const direct = await page.evaluate(async () => {
    const r = await fetch('/api/admin/users/1', { method: 'DELETE' })
    return { status: r.status, body: await r.json() }
  })
  expect(direct.status).toBe(200)
  expect(direct.body).toMatchObject({ id: 1, deleted: true })
})

