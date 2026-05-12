/**
 * mocks/handlers/redirect.handlers.ts
 * Handlers cho Redirect Service (public):
 *   GET /:shortCode  → 302 redirect (hoặc 404 / 410)
 *
 * Lưu ý: Handler này được đặt CUỐI CÙNG trong danh sách vì pattern `/:shortCode`
 * rất rộng và có thể khớp với các route khác nếu đặt trước.
 */

import { http, HttpResponse } from "msw";
import { db, isExpired, now, nextId, errorResponse } from "./db";

export const redirectHandlers = [
  // ── GET /{shortCode} — Redirect công khai ─────────────────────────────────
  http.get("/:shortCode", ({ params, request }) => {
    const { shortCode } = params as { shortCode: string };

    // Bỏ qua các route hệ thống để tránh conflict
    const ignoredPrefixes = ["api", "actuator", "favicon", "_", "static"];
    if (ignoredPrefixes.some((p) => shortCode.startsWith(p))) return;

    const url = db.urls.find((u) => u.shortCode === shortCode);

    // Bloom filter simulation: short code không tồn tại → 404
    if (!url) {
      return HttpResponse.json(
        errorResponse("NOT_FOUND", `Short code "${shortCode}" không tồn tại.`),
        { status: 404 },
      );
    }

    // URL đã bị xoá (soft-delete)
    if (!url.isActive) {
      return HttpResponse.json(
        errorResponse("NOT_FOUND", `Short code "${shortCode}" không tồn tại.`),
        { status: 404 },
      );
    }

    // URL đã hết hạn → 410 Gone
    if (isExpired(url)) {
      return HttpResponse.json(
        errorResponse("URL_EXPIRED", "URL này đã hết hạn."),
        { status: 410 },
      );
    }

    // Tăng click count
    url.clickCount += 1;
    url.updatedAt = now();

    // Ghi analytics event (mock — thực tế sẽ publish lên Kafka)
    const userAgent = request.headers.get("user-agent") ?? "";
    const referer = request.headers.get("referer") ?? null;

    db.analytics.push({
      id: nextId("analytics"),
      shortCode,
      ip: "127.0.0.1",       // mock IP
      userAgent,
      referer,
      country: "VN",          // mock country
      createdAt: now(),
    });

    // Trả về 302 Found với Location header
    return HttpResponse.redirect(url.originalUrl, 302);
  }),
];
