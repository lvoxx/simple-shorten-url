import { ApiService } from './ApiService'
import type { AuthResponse, LoginRequest, RegisterRequest, User } from '../types'

export class AuthService {
  constructor(private api: ApiService) {}

  async register(data: RegisterRequest): Promise<User> {
    return this.api.post<User>('/api/v1/auth/register', data)
  }

  async login(data: LoginRequest): Promise<AuthResponse> {
    return this.api.post<AuthResponse>('/api/v1/auth/login', data)
  }

  async refresh(): Promise<AuthResponse> {
    return this.api.post<AuthResponse>('/api/v1/auth/refresh')
  }

  async logout(): Promise<void> {
    await this.api.post('/api/v1/auth/logout')
  }
}
