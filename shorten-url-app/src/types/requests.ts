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

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
}
