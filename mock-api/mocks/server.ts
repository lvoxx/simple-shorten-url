/**
 * mocks/server.ts
 * Setup MSW cho môi trường Node.js (Jest / Vitest).
 *
 * Cách dùng trong file setup của Vitest/Jest:
 *
 *   // vitest.setup.ts hoặc jest.setup.ts
 *   import { server } from './mocks/server';
 *   import { resetDb } from './mocks/db';
 *
 *   beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
 *   afterEach(() => {
 *     server.resetHandlers();
 *     resetDb();           // reset in-memory DB sau mỗi test
 *   });
 *   afterAll(() => server.close());
 *
 * Override handler trong một test cụ thể:
 *
 *   import { http, HttpResponse } from 'msw';
 *   import { server } from '@/mocks/server';
 *
 *   it('handles 500 error', () => {
 *     server.use(
 *       http.post('/api/v1/auth/login', () =>
 *         HttpResponse.json({ message: 'Server error' }, { status: 500 })
 *       )
 *     );
 *     // ... your test
 *   });
 */

import { setupServer } from "msw/node";
import { urlHandlers } from "./url.handlers";

export const server = setupServer(...urlHandlers);
