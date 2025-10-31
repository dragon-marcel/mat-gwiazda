import { setupServer } from 'msw/node'
import { handlers } from './handlers'

declare global {
  // allow storing server singleton on globalThis
  var __mswServer__: ReturnType<typeof setupServer> | undefined
}

// Create or reuse singleton server so multiple imports point to the same instance
// No wrappers here; use MSW's native resetHandlers to restore initial handlers
const getServer = () => {
  // @ts-ignore
  if (!globalThis.__mswServer__) {
    // @ts-ignore
    globalThis.__mswServer__ = setupServer(...handlers)
  }
  // @ts-ignore
  return globalThis.__mswServer__
}

export const server = getServer()
export default server
