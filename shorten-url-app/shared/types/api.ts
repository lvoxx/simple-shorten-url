// ============================================================
// Domain Models (mirroring Spring Boot entity shapes)
// ============================================================

export interface User {
  id: number
  username: string
  email: string
  role: string
  isActive: boolean
  createdAt: string
}

export interface Url {
  id: number
  shortCode: string
  shortUrl: string
  originalUrl: string
  title: string | null
  isActive: boolean
  clickCount: number
  expireAt: string | null
  createdAt: string
}

// ============================================================
// Request DTOs
// ============================================================

export interface CreateUrlRequest {
  originalUrl: string
  title?: string | null
  expireAt?: string | null
}

export interface UpdateUrlRequest {
  title?: string | null
  expireAt?: string | null
  isActive?: boolean | null
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
}

export interface LoginRequest {
  username: string
  password: string
}

// ============================================================
// Response DTOs
// ============================================================

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export interface CursorPage<T> {
  content: T[]
  nextCursor: number | null
  hasNext: boolean
}

export interface PageResponse<T> {
  content: T[]
  total: number
  page: number
  size: number
}

// ============================================================
// Error Types (ProblemDetail / RFC 7807)
// ============================================================

export interface ProblemDetail {
  type: string
  title: string
  status: number
  detail: string
  instance: string
  errors?: string[]
}
