import { setupServer } from 'msw/node'
import { handlers } from './handlers'

declare global {
  // extend GlobalThis to include our MSW server singleton
  interface GlobalThis {
    __mswServer__?: ReturnType<typeof setupServer>
  }
}

// Create or reuse singleton server so multiple imports point to the same instance
// No wrappers here; use MSW's native resetHandlers to restore initial handlers
const getServer = () => {
  if (!globalThis.__mswServer__) {
    globalThis.__mswServer__ = setupServer(...handlers)
  }
  return globalThis.__mswServer__
}

export const server = getServer()
export default server
