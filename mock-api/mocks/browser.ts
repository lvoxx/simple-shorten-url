/**
 * mocks/browser.ts
 * Setup MSW cho môi trường browser (Vite / CRA / Next.js client-side).
 *
 * Cách dùng:
 *   1. Chạy: npx msw init public/ --save
 *   2. Trong main.tsx / index.tsx:
 *
 *      async function enableMocking() {
 *        if (process.env.NODE_ENV !== 'development') return;
 *        const { worker } = await import('./mocks/browser');
 *        return worker.start({ onUnhandledRequest: 'warn' });
 *      }
 *
 *      enableMocking().then(() => {
 *        ReactDOM.createRoot(document.getElementById('root')!).render(<App />);
 *      });
 */

import { setupWorker } from "msw/browser";
import { urlHandlers } from "./url.handlers";

export const worker = setupWorker(...urlHandlers);
