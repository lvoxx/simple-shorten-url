/**
 * mocks/handlers/index.ts
 * Kết hợp tất cả handlers theo đúng thứ tự ưu tiên.
 *
 * THỨ TỰ QUAN TRỌNG:
 *   1. healthHandlers  — route cụ thể /actuator/**
 *   2. authHandlers    — route cụ thể /api/v1/auth/**
 *   3. urlHandlers     — route cụ thể /api/v1/urls/**
 *   4. redirectHandlers — route rộng /:shortCode (phải đặt cuối)
 */

export { authHandlers } from "./auth.handlers";
export { urlHandlers } from "./url.handlers";
export { redirectHandlers } from "./redirect.handlers";
export { healthHandlers } from "./health.handlers";

import { authHandlers } from "./auth.handlers";
import { urlHandlers } from "./url.handlers";
import { redirectHandlers } from "./redirect.handlers";
import { healthHandlers } from "./health.handlers";

export const handlers = [
  ...healthHandlers,
  ...authHandlers,
  ...urlHandlers,
  ...redirectHandlers,  // ← luôn đặt cuối
];
