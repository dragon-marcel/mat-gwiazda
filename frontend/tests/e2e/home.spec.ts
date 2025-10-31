import { test, expect } from '@playwright/test'
import { setupMockApi } from './mockServer'

test('home page loads (smoke)', async ({ page }) => {
  // Setup mock API routes so tests work with backend offline
  await setupMockApi(page)

  // goto baseURL (playwright.config sets baseURL to http://localhost:5174)
  const response = await page.goto('/')
  expect(response).not.toBeNull()
  expect(response && response.ok()).toBeTruthy()
})
