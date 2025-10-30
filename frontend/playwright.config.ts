import { defineConfig, devices } from '@playwright/test';
import path from 'path';

export default defineConfig({
  testDir: 'tests/e2e',
  fullyParallel: true,
  timeout: 30 * 1000,
  expect: {
    timeout: 5000,
  },
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    actionTimeout: 0,
    baseURL: 'http://localhost:5174',
    trace: 'on-first-retry',
    headless: true,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command: 'npm run dev',
    cwd: path.resolve(__dirname),
    port: 5174,
    reuseExistingServer: process.env.CI ? false : true,
  },
});
