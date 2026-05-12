/**
 * mocks/__tests__/example.test.ts
 * Ví dụ minh hoạ cách viết test với MSW mock server.
 * Dùng Vitest (cú pháp tương tự Jest).
 *
 * Cài đặt:
 *   npm install -D msw vitest @testing-library/react
 */

import { describe, it, expect, beforeAll, afterAll, afterEach } from "vitest";
import { server } from "../server";
import { resetDb } from "../db";

// ─── Setup ────────────────────────────────────────────────────────────────────

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => { server.resetHandlers(); resetDb(); });
afterAll(() => server.close());

// ─── Helper: fetch wrapper ────────────────────────────────────────────────────

const BASE = "http://localhost";

async function api(path: string, init?: RequestInit) {
  const res = await fetch(`${BASE}${path}`, {
    headers: { "Content-Type": "application/json", ...init?.headers },
    ...init,
  });
  const body = await res.json().catch(() => null);
  return { status: res.status, body };
}

async function login(email = "demo@example.com", password = "Demo@1234") {
  const { body } = await api("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
  return body?.data?.accessToken as string;
}

// ─── Auth tests ───────────────────────────────────────────────────────────────

describe("POST /api/v1/auth/register", () => {
  it("đăng ký thành công → 201", async () => {
    const { status, body } = await api("/api/v1/auth/register", {
      method: "POST",
      body: JSON.stringify({
        email: "new@test.com",
        username: "newuser",
        password: "Pass@1234",
      }),
    });
    expect(status).toBe(201);
    expect(body.success).toBe(true);
    expect(body.data.email).toBe("new@test.com");
  });

  it("email trùng → 409", async () => {
    const { status, body } = await api("/api/v1/auth/register", {
      method: "POST",
      body: JSON.stringify({
        email: "demo@example.com", // đã seed
        username: "uniqueuser",
        password: "Pass@1234",
      }),
    });
    expect(status).toBe(409);
    expect(body.error.code).toBe("EMAIL_ALREADY_EXISTS");
  });

  it("password yếu → 400", async () => {
    const { status, body } = await api("/api/v1/auth/register", {
      method: "POST",
      body: JSON.stringify({ email: "x@x.com", username: "abc", password: "weak" }),
    });
    expect(status).toBe(400);
    expect(body.error.code).toBe("WEAK_PASSWORD");
  });
});

describe("POST /api/v1/auth/login", () => {
  it("đăng nhập thành công → 200 + tokens", async () => {
    const { status, body } = await api("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify({ email: "demo@example.com", password: "Demo@1234" }),
    });
    expect(status).toBe(200);
    expect(body.data.accessToken).toBeDefined();
    expect(body.data.refreshToken).toBeDefined();
    expect(body.data.tokenType).toBe("Bearer");
  });

  it("sai password → 401", async () => {
    const { status, body } = await api("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify({ email: "demo@example.com", password: "wrong" }),
    });
    expect(status).toBe(401);
    expect(body.error.code).toBe("INVALID_CREDENTIALS");
  });
});

// ─── URL tests ────────────────────────────────────────────────────────────────

describe("POST /api/v1/urls", () => {
  it("tạo short URL thành công → 201", async () => {
    const token = await login();
    const { status, body } = await api("/api/v1/urls", {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
      body: JSON.stringify({ originalUrl: "https://openai.com" }),
    });
    expect(status).toBe(201);
    expect(body.data.shortCode).toBeDefined();
    expect(body.data.shortUrl).toContain("yourdomain.com");
  });

  it("domain bị blacklist → 400 BLACKLISTED_DOMAIN", async () => {
    const token = await login();
    const { status, body } = await api("/api/v1/urls", {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
      body: JSON.stringify({ originalUrl: "https://malware.io/page" }),
    });
    expect(status).toBe(400);
    expect(body.error.code).toBe("BLACKLISTED_DOMAIN");
  });

  it("alias trùng → 409 ALIAS_ALREADY_EXISTS", async () => {
    const token = await login();
    // Tạo lần đầu
    await api("/api/v1/urls", {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
      body: JSON.stringify({ originalUrl: "https://example.com", alias: "mylink" }),
    });
    // Tạo lần 2 với alias trùng
    const { status, body } = await api("/api/v1/urls", {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
      body: JSON.stringify({ originalUrl: "https://other.com", alias: "mylink" }),
    });
    expect(status).toBe(409);
    expect(body.error.code).toBe("ALIAS_ALREADY_EXISTS");
  });

  it("không có token → 401", async () => {
    const { status } = await api("/api/v1/urls", {
      method: "POST",
      body: JSON.stringify({ originalUrl: "https://example.com" }),
    });
    expect(status).toBe(401);
  });
});

describe("GET /api/v1/urls", () => {
  it("trả về danh sách URL của user → 200", async () => {
    const token = await login();
    const { status, body } = await api("/api/v1/urls", {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(status).toBe(200);
    expect(Array.isArray(body.data.content)).toBe(true);
    expect(body.data.totalElements).toBeGreaterThan(0);
  });
});

describe("DELETE /api/v1/urls/:shortCode", () => {
  it("xoá URL của chính mình → 204", async () => {
    const token = await login();
    const { status } = await api("/api/v1/urls/4c92", {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(status).toBe(204);
  });

  it("xoá URL đã xoá → 404", async () => {
    const token = await login();
    await api("/api/v1/urls/4c92", {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });
    const { status } = await api("/api/v1/urls/4c92", {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(status).toBe(404);
  });
});

// ─── Redirect tests ───────────────────────────────────────────────────────────

describe("GET /:shortCode (Redirect Service)", () => {
  it("short code hợp lệ → 302", async () => {
    const res = await fetch(`${BASE}/4c92`, { redirect: "manual" });
    expect(res.status).toBe(302);
    expect(res.headers.get("location")).toBe(
      "https://www.example.com/very/long/path?with=params",
    );
  });

  it("short code không tồn tại → 404", async () => {
    const { status } = await api("/nonexistent");
    expect(status).toBe(404);
  });

  it("short code đã hết hạn → 410", async () => {
    const { status, body } = await api("/exp1");
    expect(status).toBe(410);
    expect(body.error.code).toBe("URL_EXPIRED");
  });
});
