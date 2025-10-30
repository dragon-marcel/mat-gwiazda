import { test, expect } from '@playwright/test';

test('app loads and redirects to /play', async ({ page, baseURL }) => {
  await page.goto('/');
  // App should redirect to /play per routing
  await expect(page).toHaveURL(/\/play/);
  // basic smoke: check that root element exists
  await expect(page.locator('#root')).toBeVisible();
});

