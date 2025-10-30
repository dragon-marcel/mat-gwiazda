import '@testing-library/jest-dom';
import { beforeAll, afterAll, afterEach } from 'vitest';
import { server } from './mocks/server';

// Start MSW before all tests
beforeAll(() => server.listen());
// Reset handlers after each test to avoid test leakage
afterEach(() => server.resetHandlers());
// Clean up after the tests are finished
afterAll(() => server.close());

