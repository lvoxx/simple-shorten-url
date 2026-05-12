/**
 * mocks/db.ts
 * In-memory store — simulates PostgreSQL + Redis state for MSW handlers.
 * Reset bằng cách gọi resetDb() giữa các test.
 */

// ─── Types ────────────────────────────────────────────────────────────────────

export interface User {
  id: number;
  email: string;
  username: string;
  passwordHash: string; // plain-text trong mock để dễ so sánh
  role: "USER" | "ADMIN";
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Url {
  id: number;
  shortCode: string;
  originalUrl: string;
  userId: number | null;
  title: string | null;
  alias: string | null;
  isActive: boolean;
  clickCount: number;
  expireAt: string | null;
  createdAt: string;
  updatedAt: string;
  createBy: string;
  updateBy: string;
}

export interface RefreshToken {
  id: number;
  userId: number;
  token: string;
  expiresAt: string;
  isRevoked: boolean;
  createdAt: string;
}

export interface Analytics {
  id: number;
  shortCode: string;
  ip: string;
  userAgent: string;
  referer: string | null;
  country: string | null;
  createdAt: string;
}

export interface DomainBlacklist {
  id: number;
  domain: string;
  reason: string | null;
  createdAt: string;
}

// ─── Store ────────────────────────────────────────────────────────────────────

let _idCounter = { user: 1, url: 100, token: 1, analytics: 1 };

export const db = {
  users: [] as User[],
  urls: [] as Url[],
  refreshTokens: [] as RefreshToken[],
  analytics: [] as Analytics[],
  blacklist: [
    { id: 1, domain: "malware.io", reason: "phishing", createdAt: now() },
    { id: 2, domain: "spam-site.com", reason: "spam", createdAt: now() },
  ] as DomainBlacklist[],
};

// ─── Seed data ────────────────────────────────────────────────────────────────

export function seedDefaults() {
  const user: User = {
    id: nextId("user"),
    email: "demo@example.com",
    username: "demo",
    passwordHash: "Demo@1234",
    role: "USER",
    isActive: true,
    createdAt: now(),
    updatedAt: now(),
  };
  db.users.push(user);

  const adminUser: User = {
    id: nextId("user"),
    email: "admin@example.com",
    username: "admin",
    passwordHash: "Admin@1234",
    role: "ADMIN",
    isActive: true,
    createdAt: now(),
    updatedAt: now(),
  };
  db.users.push(adminUser);

  // Seed một vài URLs
  const sampleUrls: Omit<Url, "id">[] = [
    {
      shortCode: "4c92",
      originalUrl: "https://www.example.com/very/long/path?with=params",
      userId: user.id,
      title: "Example",
      alias: null,
      isActive: true,
      clickCount: 1500,
      expireAt: null,
      createdAt: "2024-01-01T00:00:00Z",
      updatedAt: "2024-01-01T00:00:00Z",
      createBy: "Annonymous",
      updateBy: "Annonymous",
    },
    {
      shortCode: "xYz1",
      originalUrl: "https://github.com/some/repo",
      userId: user.id,
      title: "GitHub Repo",
      alias: "github",
      isActive: true,
      clickCount: 300,
      expireAt: null,
      createdAt: "2024-02-01T00:00:00Z",
      updatedAt: "2024-02-01T00:00:00Z",
      createBy: "Annonymous",
      updateBy: "Annonymous",
    },
    {
      shortCode: "exp1",
      originalUrl: "https://expired-link.com",
      userId: user.id,
      title: "Expired link",
      alias: null,
      isActive: true,
      clickCount: 10,
      expireAt: "2020-01-01T00:00:00Z", // đã hết hạn
      createdAt: "2020-01-01T00:00:00Z",
      updatedAt: "2020-01-01T00:00:00Z",
      createBy: "Annonymous",
      updateBy: "Annonymous",
    },
  ];
  sampleUrls.forEach((u) => db.urls.push({ id: nextId("url"), ...u }));

  // Seed analytics cho "4c92"
  for (let i = 0; i < 5; i++) {
    db.analytics.push({
      id: nextId("analytics"),
      shortCode: "4c92",
      ip: `192.168.1.${i + 1}`,
      userAgent: "Mozilla/5.0",
      referer: i % 2 === 0 ? "https://google.com" : null,
      country: ["US", "VN", "JP", "DE", "GB"][i],
      createdAt: `2024-01-0${i + 1}T00:00:00Z`,
    });
  }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

export function nextId(entity: keyof typeof _idCounter): number {
  return _idCounter[entity]++;
}

export function now(): string {
  return new Date().toISOString();
}

/** Tạo JWT-like access token giả */
export function makeAccessToken(userId: number): string {
  const payload = btoa(JSON.stringify({ sub: String(userId), exp: Date.now() + 900_000 }));
  return `eyJhbGciOiJIUzI1NiJ9.${payload}.mock_signature`;
}

/** Parse userId từ mock access token */
export function parseAccessToken(token: string): number | null {
  try {
    const parts = token.split(".");
    if (parts.length !== 3) return null;
    const payload = JSON.parse(atob(parts[1]));
    if (payload.exp < Date.now()) return null;
    return Number(payload.sub);
  } catch {
    return null;
  }
}

export function makeRefreshToken(): string {
  return `mock_refresh_${Math.random().toString(36).slice(2)}`;
}

/** Base62 encode số để tạo shortCode (đơn giản hoá) */
export function encodeBase62(num: number): string {
  const chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  let result = "";
  while (num > 0) {
    result = chars[num % 62] + result;
    num = Math.floor(num / 62);
  }
  return result || "0";
}

/** Kiểm tra URL có trong blacklist không */
export function isDomainBlacklisted(url: string): boolean {
  try {
    const hostname = new URL(url).hostname;
    return db.blacklist.some((b) => b.domain === hostname);
  } catch {
    return false;
  }
}

/** Kiểm tra URL có hết hạn không */
export function isExpired(url: Url): boolean {
  if (!url.expireAt) return false;
  return new Date(url.expireAt) < new Date();
}

/** Reset toàn bộ store về state ban đầu */
export function resetDb() {
  db.users.length = 0;
  db.urls.length = 0;
  db.refreshTokens.length = 0;
  db.analytics.length = 0;
  db.blacklist.length = 0;
  db.blacklist.push(
    { id: 1, domain: "malware.io", reason: "phishing", createdAt: now() },
    { id: 2, domain: "spam-site.com", reason: "spam", createdAt: now() },
  );
  _idCounter = { user: 1, url: 100, token: 1, analytics: 1 };
  seedDefaults();
}

// ─── Response envelope helpers ────────────────────────────────────────────────

export function successResponse<T>(data: T) {
  return { success: true, data, timestamp: now() };
}

export function errorResponse(code: string, message: string, details: unknown = null) {
  return {
    success: false,
    error: { code, message, details },
    timestamp: now(),
  };
}

// Khởi tạo seed khi import module
seedDefaults();
