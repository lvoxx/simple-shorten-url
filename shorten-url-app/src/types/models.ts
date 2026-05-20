export interface UserResponse {
  id: number
  username: string
  email: string
  role: string
  isActive: boolean
  createdAt: string
}

export interface UrlResponse {
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

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: UserResponse
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

export interface AnalyticsResponse {
  id: number
  shortCode: string
  ip: string | null
  userAgent: string | null
  referer: string | null
  country: string | null
  createdAt: string
}

export interface DomainBlacklistResponse {
  id: number
  domain: string
  reason: string | null
  createdAt: string
}
