import '@testing-library/jest-dom'
// Use the project's MSW server
import { server } from './mocks/server'

// Debug logs to help trace handler registration
// eslint-disable-next-line no-console
console.log('[msw] setupTests importing server')

// Lifecycle hooks for MSW (use the shared server)
beforeAll(() => {
  // eslint-disable-next-line no-console
  console.log('[msw] setupTests beforeAll: starting server')
  server.listen({ onUnhandledRequest: 'warn' })
})
afterEach(() => {
  // eslint-disable-next-line no-console
  console.log('[msw] setupTests afterEach: resetting handlers')
  server.resetHandlers()
})
afterAll(() => {
  // eslint-disable-next-line no-console
  console.log('[msw] setupTests afterAll: closing server')
  server.close()
})
