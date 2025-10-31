// vitest.config.cjs
module.exports = {
  test: {
    environment: 'jsdom',
    globals: true,
    // Use the TypeScript setup file if present
    setupFiles: ['./src/setupTests.ts'],
    // Include .jsx test files as well
    include: [
      'src/**/*.test.{js,jsx,ts,tsx}',
      'src/**/__tests__/**/*.{js,jsx,ts,tsx}',
      '**/*.test.jsx',
      '**/__tests__/**/*.jsx'
    ],
    coverage: {
      provider: 'istanbul',
      reporter: ['text', 'lcov'],
    },
  },
};

