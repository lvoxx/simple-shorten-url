/**
 * mocks/handlers/url.handlers.ts
 * Handlers cho các endpoint quản lý URL:
 *   POST   /api/v1/urls
 *   GET    /api/v1/urls
 *   GET    /api/v1/urls/:shortCode
 *   DELETE /api/v1/urls/:shortCode
 *   GET    /api/v1/urls/:shortCode/analytics
 */

import { http, HttpResponse } from "msw";
import {
  db,
  nextId,
  now,
  encodeBase62,
  isDomainBlacklisted,
  isExpired,
  parseAccessToken,
  errorResponse,
  successResponse,
  type Url,
} from "./db";

const BASE = "/api/v1/urls";

// ─── Middleware helper: lấy userId từ Authorization header ────────────────────

function getUserIdFromRequest(request: Request): number | null {
  const authHeader = request.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) return null;
  return parseAccessToken(authHeader.slice(7));
}

// ─── URL response shape ───────────────────────────────────────────────────────

function toUrlDto(url: Url) {
  return {
    id: url.id,
    shortCode: url.shortCode,
    shortUrl: `https://yourdomain.com/${url.shortCode}`,
    originalUrl: url.originalUrl,
    alias: url.alias,
    title: url.title,
    clickCount: url.clickCount,
    expireAt: url.expireAt,
    createdAt: url.createdAt,
    updatedAt: url.updatedAt,
  };
}

// ─── Handlers ─────────────────────────────────────────────────────────────────

export const urlHandlers = [
  // ── POST /api/v1/urls — Tạo short URL mới ────────────────────────────────
  http.post(BASE, async ({ request }) => {
    const userId = getUserIdFromRequest(request);
    if (!userId) {
      return HttpResponse.json(
        errorResponse("UNAUTHORIZED", "Yêu cầu đăng nhập."),
        { status: 401 },
      );
    }

    const body = (await request.json()) as {
      originalUrl?: string;
      alias?: string;
      expireAt?: string;
    };

    const { originalUrl, alias, expireAt } = body;

    // Validate URL
    if (!originalUrl) {
      return HttpResponse.json(
        errorResponse("INVALID_URL", "originalUrl là bắt buộc."),
        { status: 400 },
      );
    }

    let parsedUrl: URL;
    try {
      parsedUrl = new URL(originalUrl);
    } catch {
      return HttpResponse.json(
        errorResponse("INVALID_URL", "URL không đúng định dạng."),
        { status: 400 },
      );
    }

    if (!["http:", "https:"].includes(parsedUrl.protocol)) {
      return HttpResponse.json(
        errorResponse("INVALID_URL", "Chỉ hỗ trợ scheme http và https."),
        { status: 400 },
      );
    }

    if (originalUrl.length > 2048) {
      return HttpResponse.json(
        errorResponse("INVALID_URL", "URL quá dài (tối đa 2048 ký tự)."),
        { status: 400 },
      );
    }

    // Blacklist check
    if (isDomainBlacklisted(originalUrl)) {
      return HttpResponse.json(
        errorResponse("BLACKLISTED_DOMAIN", "Domain này nằm trong danh sách bị chặn."),
        { status: 400 },
      );
    }

    // Custom alias check
    if (alias) {
      const aliasExists = db.urls.some((u) => u.alias === alias && u.isActive);
      if (aliasExists) {
        return HttpResponse.json(
          errorResponse("ALIAS_ALREADY_EXISTS", `Alias "${alias}" đã được sử dụng.`),
          { status: 409 },
        );
      }
    }

    // Validate expireAt nếu có
    if (expireAt && isNaN(Date.parse(expireAt))) {
      return HttpResponse.json(
        errorResponse("INVALID_DATE", "expireAt không đúng định dạng ISO 8601."),
        { status: 400 },
      );
    }

    const id = nextId("url");
    const shortCode = alias ?? encodeBase62(id);

    const newUrl: Url = {
      id,
      shortCode,
      originalUrl,
      userId,
      title: null,
      alias: alias ?? null,
      isActive: true,
      clickCount: 0,
      expireAt: expireAt ?? null,
      createdAt: now(),
      updatedAt: now(),
      createBy: "Annonymous",
      updateBy: "Annonymous",
    };

    db.urls.push(newUrl);

    return HttpResponse.json(successResponse(toUrlDto(newUrl)), { status: 201 });
  }),

  // ── GET /api/v1/urls — Danh sách URL của user ─────────────────────────────
  http.get(BASE, ({ request }) => {
    const userId = getUserIdFromRequest(request);
    if (!userId) {
      return HttpResponse.json(
        errorResponse("UNAUTHORIZED", "Yêu cầu đăng nhập."),
        { status: 401 },
      );
    }

    const url = new URL(request.url);
    const page = parseInt(url.searchParams.get("page") ?? "0", 10);
    const size = Math.min(parseInt(url.searchParams.get("size") ?? "20", 10), 100);
    const sort = url.searchParams.get("sort") ?? "createdAt,desc";

    const userUrls = db.urls.filter((u) => u.userId === userId && u.isActive);

    // Sort
    const [sortField, sortDir] = sort.split(",");
    userUrls.sort((a, b) => {
      const aVal = (a as unknown as Record<string, unknown>)[sortField] as string;
      const bVal = (b as unknown as Record<string, unknown>)[sortField] as string;
      return sortDir === "desc"
        ? bVal?.localeCompare(aVal)
        : aVal?.localeCompare(bVal);
    });

    const totalElements = userUrls.length;
    const totalPages = Math.ceil(totalElements / size);
    const content = userUrls.slice(page * size, page * size + size).map(toUrlDto);

    return HttpResponse.json(
      successResponse({ content, page, size, totalElements, totalPages }),
    );
  }),

  // ── GET /api/v1/urls/:shortCode — Chi tiết một URL ────────────────────────
  http.get(`${BASE}/:shortCode`, ({ request, params }) => {
    const userId = getUserIdFromRequest(request);
    if (!userId) {
      return HttpResponse.json(
        errorResponse("UNAUTHORIZED", "Yêu cầu đăng nhập."),
        { status: 401 },
      );
    }

    // Bỏ qua route analytics (có suffix riêng)
    if ((params.shortCode as string).includes("/analytics")) return;

    const url = db.urls.find(
      (u) => u.shortCode === params.shortCode && u.isActive,
    );

    if (!url) {
      return HttpResponse.json(
        errorResponse("NOT_FOUND", "Short URL không tồn tại."),
        { status: 404 },
      );
    }

    // Non-owner hoặc non-admin không được xem
    const user = db.users.find((u) => u.id === userId);
    if (url.userId !== userId && user?.role !== "ADMIN") {
      return HttpResponse.json(
        errorResponse("FORBIDDEN", "Bạn không có quyền truy cập URL này."),
        { status: 403 },
      );
    }

    return HttpResponse.json(successResponse(toUrlDto(url)));
  }),

  // ── DELETE /api/v1/urls/:shortCode — Xoá mềm URL ─────────────────────────
  http.delete(`${BASE}/:shortCode`, ({ request, params }) => {
    const userId = getUserIdFromRequest(request);
    if (!userId) {
      return HttpResponse.json(
        errorResponse("UNAUTHORIZED", "Yêu cầu đăng nhập."),
        { status: 401 },
      );
    }

    const url = db.urls.find(
      (u) => u.shortCode === params.shortCode,
    );

    if (!url || !url.isActive) {
      return HttpResponse.json(
        errorResponse("NOT_FOUND", "Short URL không tồn tại hoặc đã bị xoá."),
        { status: 404 },
      );
    }

    const user = db.users.find((u) => u.id === userId);
    if (url.userId !== userId && user?.role !== "ADMIN") {
      return HttpResponse.json(
        errorResponse("FORBIDDEN", "Bạn không có quyền xoá URL này."),
        { status: 403 },
      );
    }

    // Soft delete
    url.isActive = false;
    url.updatedAt = now();

    return new HttpResponse(null, { status: 204 });
  }),

  // ── GET /api/v1/urls/:shortCode/analytics ─────────────────────────────────
  http.get(`${BASE}/:shortCode/analytics`, ({ request, params }) => {
    const userId = getUserIdFromRequest(request);
    if (!userId) {
      return HttpResponse.json(
        errorResponse("UNAUTHORIZED", "Yêu cầu đăng nhập."),
        { status: 401 },
      );
    }

    const url = db.urls.find(
      (u) => u.shortCode === params.shortCode && u.isActive,
    );

    if (!url) {
      return HttpResponse.json(
        errorResponse("NOT_FOUND", "Short URL không tồn tại."),
        { status: 404 },
      );
    }

    const user = db.users.find((u) => u.id === userId);
    if (url.userId !== userId && user?.role !== "ADMIN") {
      return HttpResponse.json(
        errorResponse("FORBIDDEN", "Bạn không có quyền xem analytics URL này."),
        { status: 403 },
      );
    }

    const urlObj = new URL(request.url);
    const from = urlObj.searchParams.get("from");
    const to = urlObj.searchParams.get("to");
    const groupBy = urlObj.searchParams.get("groupBy") ?? "day";

    let rows = db.analytics.filter((a) => a.shortCode === params.shortCode);

    if (from) rows = rows.filter((r) => r.createdAt >= from);
    if (to) rows = rows.filter((r) => r.createdAt <= to);

    const totalClicks = rows.length;

    // Group by date (simplified: dùng date prefix)
    const grouped: Record<string, number> = {};
    rows.forEach((r) => {
      const key =
        groupBy === "month"
          ? r.createdAt.slice(0, 7)
          : groupBy === "hour"
            ? r.createdAt.slice(0, 13)
            : r.createdAt.slice(0, 10);
      grouped[key] = (grouped[key] ?? 0) + 1;
    });

    const series = Object.entries(grouped)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([date, clicks]) => ({ date, clicks }));

    // Top countries
    const countryCounts: Record<string, number> = {};
    rows.forEach((r) => {
      if (r.country) countryCounts[r.country] = (countryCounts[r.country] ?? 0) + 1;
    });
    const topCountries = Object.entries(countryCounts)
      .sort(([, a], [, b]) => b - a)
      .slice(0, 5)
      .map(([country, clicks]) => ({ country, clicks }));

    return HttpResponse.json(
      successResponse({
        shortCode: params.shortCode,
        totalClicks,
        period: {
          from: from ?? rows[0]?.createdAt?.slice(0, 10) ?? null,
          to: to ?? rows[rows.length - 1]?.createdAt?.slice(0, 10) ?? null,
        },
        series,
        topCountries,
      }),
    );
  }),
];
