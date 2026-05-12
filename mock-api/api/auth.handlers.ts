/**
 * mocks/handlers/auth.handlers.ts
 * Handlers cho các endpoint xác thực:
 *   POST /api/v1/auth/register
 *   POST /api/v1/auth/login
 *   POST /api/v1/auth/refresh
 *   POST /api/v1/auth/logout
 */

import { http, HttpResponse } from "msw";
import {
  db,
  nextId,
  now,
  makeAccessToken,
  makeRefreshToken,
  parseAccessToken,
  errorResponse,
  successResponse,
} from "./db";

const BASE = "/api/v1/auth";

// ── Validation helpers ────────────────────────────────────────────────────────

function isValidEmail(email: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function isStrongPassword(password: string) {
  // min 8 chars, at least one uppercase, one digit
  return password.length >= 8 && /[A-Z]/.test(password) && /\d/.test(password);
}

function isValidUsername(username: string) {
  return /^[a-zA-Z0-9_]{3,50}$/.test(username);
}

// ─── Handlers ─────────────────────────────────────────────────────────────────

export const authHandlers = [
  // ── POST /api/v1/auth/register ────────────────────────────────────────────
  http.post(`${BASE}/register`, async ({ request }) => {
    const body = (await request.json()) as {
      email?: string;
      username?: string;
      password?: string;
    };

    const { email, username, password } = body;

    // Validation
    if (!email || !isValidEmail(email)) {
      return HttpResponse.json(
        errorResponse("INVALID_EMAIL", "Email không hợp lệ."),
        { status: 400 },
      );
    }
    if (!username || !isValidUsername(username)) {
      return HttpResponse.json(
        errorResponse(
          "INVALID_USERNAME",
          "Username phải 3–50 ký tự, chỉ chứa chữ cái, số hoặc dấu gạch dưới.",
        ),
        { status: 400 },
      );
    }
    if (!password || !isStrongPassword(password)) {
      return HttpResponse.json(
        errorResponse(
          "WEAK_PASSWORD",
          "Password phải tối thiểu 8 ký tự, có ít nhất 1 chữ hoa và 1 chữ số.",
        ),
        { status: 400 },
      );
    }

    // Duplicate check
    if (db.users.some((u) => u.email === email)) {
      return HttpResponse.json(
        errorResponse("EMAIL_ALREADY_EXISTS", "Email đã được đăng ký."),
        { status: 409 },
      );
    }
    if (db.users.some((u) => u.username === username)) {
      return HttpResponse.json(
        errorResponse("USERNAME_ALREADY_EXISTS", "Username đã tồn tại."),
        { status: 409 },
      );
    }

    const user = {
      id: nextId("user"),
      email,
      username,
      passwordHash: password, // mock: lưu plain-text
      role: "USER" as const,
      isActive: true,
      createdAt: now(),
      updatedAt: now(),
    };
    db.users.push(user);

    return HttpResponse.json(
      successResponse({
        id: user.id,
        email: user.email,
        username: user.username,
        createdAt: user.createdAt,
      }),
      { status: 201 },
    );
  }),

  // ── POST /api/v1/auth/login ───────────────────────────────────────────────
  http.post(`${BASE}/login`, async ({ request }) => {
    const body = (await request.json()) as {
      email?: string;
      password?: string;
    };

    const { email, password } = body;

    if (!email || !password) {
      return HttpResponse.json(
        errorResponse("MISSING_CREDENTIALS", "Email và password là bắt buộc."),
        { status: 400 },
      );
    }

    const user = db.users.find((u) => u.email === email);

    if (!user || user.passwordHash !== password) {
      return HttpResponse.json(
        errorResponse("INVALID_CREDENTIALS", "Email hoặc password không đúng."),
        { status: 401 },
      );
    }

    if (!user.isActive) {
      return HttpResponse.json(
        errorResponse("ACCOUNT_DISABLED", "Tài khoản đã bị vô hiệu hoá."),
        { status: 403 },
      );
    }

    const accessToken = makeAccessToken(user.id);
    const refreshTokenValue = makeRefreshToken();

    db.refreshTokens.push({
      id: nextId("token"),
      userId: user.id,
      token: refreshTokenValue,
      expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
      isRevoked: false,
      createdAt: now(),
    });

    return HttpResponse.json(
      successResponse({
        accessToken,
        refreshToken: refreshTokenValue,
        tokenType: "Bearer",
        expiresIn: 900,
      }),
      { status: 200 },
    );
  }),

  // ── POST /api/v1/auth/refresh ─────────────────────────────────────────────
  http.post(`${BASE}/refresh`, async ({ request }) => {
    const body = (await request.json()) as { refreshToken?: string };
    const { refreshToken } = body;

    if (!refreshToken) {
      return HttpResponse.json(
        errorResponse("MISSING_TOKEN", "refreshToken là bắt buộc."),
        { status: 400 },
      );
    }

    const stored = db.refreshTokens.find((t) => t.token === refreshToken);

    if (!stored) {
      return HttpResponse.json(
        errorResponse("INVALID_REFRESH_TOKEN", "Refresh token không hợp lệ."),
        { status: 401 },
      );
    }

    if (stored.isRevoked) {
      return HttpResponse.json(
        errorResponse("TOKEN_REVOKED", "Refresh token đã bị thu hồi."),
        { status: 401 },
      );
    }

    if (new Date(stored.expiresAt) < new Date()) {
      return HttpResponse.json(
        errorResponse("TOKEN_EXPIRED", "Refresh token đã hết hạn."),
        { status: 401 },
      );
    }

    const newAccessToken = makeAccessToken(stored.userId);

    return HttpResponse.json(
      successResponse({ accessToken: newAccessToken, expiresIn: 900 }),
      { status: 200 },
    );
  }),

  // ── POST /api/v1/auth/logout ──────────────────────────────────────────────
  http.post(`${BASE}/logout`, ({ request }) => {
    const authHeader = request.headers.get("Authorization");

    if (!authHeader?.startsWith("Bearer ")) {
      return HttpResponse.json(
        errorResponse("UNAUTHORIZED", "Yêu cầu Authorization header."),
        { status: 401 },
      );
    }

    const token = authHeader.slice(7);
    const userId = parseAccessToken(token);

    if (!userId) {
      return HttpResponse.json(
        errorResponse("INVALID_TOKEN", "Access token không hợp lệ hoặc đã hết hạn."),
        { status: 401 },
      );
    }

    // Revoke tất cả refresh tokens của user
    db.refreshTokens
      .filter((t) => t.userId === userId && !t.isRevoked)
      .forEach((t) => (t.isRevoked = true));

    return new HttpResponse(null, { status: 204 });
  }),
];
