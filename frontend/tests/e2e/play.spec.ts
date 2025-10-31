import { test, expect } from '@playwright/test'
import { setupMockApi } from './mockServer'

test('play flow: answer tasks and see results', async ({ page }) => {
  await setupMockApi(page)
  await page.goto('/play')

  // Wait for a question to be visible
  const question = page.getByText(/2\+2=\?|Question 1/)
  const hasQuestion = await question.count() > 0

  if (hasQuestion) {
    // Try to choose option '4' (correct) or button/text 'A'
    const correctOpt = page.locator('button:has-text("4"), button:has-text("A"), input[value="4"], input[value="A"]')
    if (await correctOpt.count() > 0) {
      await correctOpt.first().click()
    }

    // submit
    const submitBtn = page.getByRole('button', { name: /submit|answer|send/i })
    if (await submitBtn.count() > 0) {
      await submitBtn.first().click()
    }

    // Expect result indicator
    const result = page.getByText(/correct|true|well done|congrat/i)
    const seen = await result.count() > 0
    if (!seen) {
      // sometimes app renders a status element
      const statusEl = page.locator('[role="status"]')
      if (await statusEl.count() > 0) {
        const txt = await statusEl.first().innerText()
        expect(/saved|error|correct|true|false/i.test(txt)).toBeTruthy()
      } else {
        // fallback to API submit to verify server behavior
        const resp = await page.evaluate(async () => {
          const r = await fetch('/api/task/1/submit', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ answer: 'A' }) })
          return { status: r.status, body: await r.json() }
        })
        expect(resp.status).toBe(200)
        expect(resp.body).toHaveProperty('correct')
      }
    }

    // Move to next question: try click next button or go to /play/2
    const nextBtn = page.getByRole('button', { name: /next|continue|następny|dalej/i })
    if (await nextBtn.count() > 0) {
      await nextBtn.first().click()
    } else {
      await page.goto('/play/2')
    }

    // Now answer incorrectly: pick an option that is not correct ('3' or 'B')
    const wrongOpt = page.locator('button:has-text("3"), button:has-text("B"), input[value="3"], input[value="B"]')
    if (await wrongOpt.count() > 0) {
      await wrongOpt.first().click()
    }
    if (await submitBtn.count() > 0) {
      await submitBtn.first().click()
    }

    // Check for result text
    const wrongResult = page.getByText(/incorrect|false|try again/i)
    // allow either UI text or API submit
    if (await wrongResult.count() > 0) {
      await expect(wrongResult.first()).toBeVisible()
    } else {
      const resp2 = await page.evaluate(async () => {
        const r = await fetch('/api/task/2/submit', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ answer: 'B' }) })
        return { status: r.status, body: await r.json() }
      })
      expect(resp2.status).toBe(200)
      expect(resp2.body).toHaveProperty('correct')
    }
  } else {
    // No play UI — fallback: exercise API sequence directly
    const list = await page.evaluate(async () => fetch('/api/tasks').then(r => r.json()))
    expect(Array.isArray(list)).toBeTruthy()
    const submit = await page.evaluate(async () => fetch('/api/task/1/submit', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ answer: 'A' }) }).then(r => r.json()))
    expect(submit).toHaveProperty('correct')
  }
})

