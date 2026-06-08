import { http } from '@/lib/http'
import type { AuthResponse, LoginRequest, RegisterRequest, UserResponse } from '@/types/api'

/**
 * Auth endpoints (/api/v1/auth, /api/v1/users/me).
 * Refresh/logout rely on the HTTP-only cookie, so they skip the Bearer header;
 * refresh additionally skips the 401-retry loop to avoid recursion.
 */
export const authService = {
  register(payload: RegisterRequest): Promise<UserResponse> {
    return http.post<UserResponse>('/v1/auth/register', payload, { skipAuth: true })
  },

  login(payload: LoginRequest): Promise<AuthResponse> {
    return http.post<AuthResponse>('/v1/auth/login', payload, { skipAuth: true })
  },

  refresh(): Promise<AuthResponse> {
    return http.post<AuthResponse>('/v1/auth/refresh', undefined, {
      skipAuth: true,
      skipRefresh: true,
    })
  },

  logout(): Promise<void> {
    return http.post<void>('/v1/auth/logout', undefined, { skipAuth: true })
  },

  me(): Promise<UserResponse> {
    return http.get<UserResponse>('/v1/users/me')
  },
}
