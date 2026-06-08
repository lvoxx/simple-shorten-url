/**
 * TypeScript mirrors of the backend DTOs (services/common/dto).
 * Timestamps are serialized by Spring as ISO-8601 strings (LocalDateTime),
 * so they are typed as `string` here and formatted via lib/format.ts.
 */

export interface UserResponse {
  id: number
  username: string
  email: string
  role: string
  isActive: boolean
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: UserResponse
}

export interface UrlResponse {
  id: number
  shortCode: string
  /** Absolute short link (already points at redirect_service). Display/copy only. */
  shortUrl: string
  originalUrl: string
  title: string | null
  isActive: boolean
  clickCount: number
  expireAt: string | null
  createdAt: string
}

export interface CursorPage<T> {
  content: T[]
  nextCursor: number | null
  hasNext: boolean
}

// ── Request payloads ──────────────────────────────────────────────

export interface RegisterRequest {
  username: string
  email: string
  password: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface CreateUrlRequest {
  originalUrl: string
  title?: string | null
  expireAt?: string | null
}

export interface UpdateUrlRequest {
  title?: string | null
  expireAt?: string | null
  isActive?: boolean
}
