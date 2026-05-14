import { describe, it, expect, vi, beforeEach } from 'vitest'
import { AuthService } from '../../shared/services/AuthService'
import { ApiService } from '../../shared/services/ApiService'
import type { AuthResponse, User } from '../../shared/types'

describe('AuthService', () => {
  let api: ApiService
  let authService: AuthService
  const mockUser: User = {
    id: 1,
    username: 'testuser',
    email: 'test@example.com',
    role: 'USER',
    isActive: true,
    createdAt: '2026-05-14T12:00:00',
  }
  const mockAuthResponse: AuthResponse = {
    accessToken: 'jwt-token-123',
    tokenType: 'Bearer',
    expiresIn: 900,
    user: mockUser,
  }

  beforeEach(() => {
    api = new ApiService()
    authService = new AuthService(api)
  })

  describe('register', () => {
    it('should POST register and return the created user', async () => {
      vi.spyOn(api, 'post').mockResolvedValue(mockUser)

      const result = await authService.register({
        username: 'testuser',
        email: 'test@example.com',
        password: 'secret123',
      })

      expect(api.post).toHaveBeenCalledWith('/api/v1/auth/register', {
        username: 'testuser',
        email: 'test@example.com',
        password: 'secret123',
      })
      expect(result).toEqual(mockUser)
    })

    it('should propagate errors on registration failure', async () => {
      const error = new Error('User already exists')
      vi.spyOn(api, 'post').mockRejectedValue(error)

      await expect(
        authService.register({
          username: 'existing',
          email: 'existing@test.com',
          password: 'secret123',
        })
      ).rejects.toThrow('User already exists')
    })
  })

  describe('login', () => {
    it('should POST login and return auth response', async () => {
      vi.spyOn(api, 'post').mockResolvedValue(mockAuthResponse)

      const result = await authService.login({
        username: 'testuser',
        password: 'secret123',
      })

      expect(api.post).toHaveBeenCalledWith('/api/v1/auth/login', {
        username: 'testuser',
        password: 'secret123',
      })
      expect(result).toEqual(mockAuthResponse)
      expect(result.accessToken).toBe('jwt-token-123')
      expect(result.user.username).toBe('testuser')
    })
  })

  describe('refresh', () => {
    it('should POST refresh and return new auth response', async () => {
      vi.spyOn(api, 'post').mockResolvedValue(mockAuthResponse)

      const result = await authService.refresh()

      expect(api.post).toHaveBeenCalledWith('/api/v1/auth/refresh')
      expect(result).toEqual(mockAuthResponse)
    })
  })

  describe('logout', () => {
    it('should POST logout successfully', async () => {
      vi.spyOn(api, 'post').mockResolvedValue(undefined)

      await authService.logout()

      expect(api.post).toHaveBeenCalledWith('/api/v1/auth/logout')
    })
  })
})
