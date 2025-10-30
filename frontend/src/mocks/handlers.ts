import { rest } from 'msw';

export const handlers = [
  // health endpoint used in some local flows
  rest.get('http://localhost:8080/api/v1/health', (req, res, ctx) => {
    return res(ctx.status(200), ctx.json({ status: 'ok' }));
  }),

  // example: auth refresh
  rest.post('http://localhost:8080/api/v1/auth/refresh', (req, res, ctx) => {
    return res(ctx.status(200), ctx.json({ accessToken: 'fake-token', refreshToken: 'fake-refresh' }));
  }),
];

