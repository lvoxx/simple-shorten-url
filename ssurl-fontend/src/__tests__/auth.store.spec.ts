import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

import { authService } from '@/services/authService'
import { useAuthStore } from '@/stores/auth'
import type { AuthResponse, UserResponse } from '@/types/api'

vi.mock('@/services/authService', () => ({
  authService: {
    login: vi.fn(),
    register: vi.fn(),
    refresh: vi.fn(),
    logout: vi.fn(),
    me: vi.fn(),
  },
}))

const user: UserResponse = {
  id: 1,
  username: 'ada',
  email: 'ada@example.com',
  role: 'USER',
  isActive: true,
  createdAt: '2026-01-01T00:00:00',
}

const authResponse: AuthResponse = {
  accessToken: 'token-123',
  tokenType: 'Bearer',
  expiresIn: 900,
  user,
}

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('starts unauthenticated', () => {
    const store = useAuthStore()
    expect(store.isAuthenticated).toBe(false)
    expect(store.accessToken).toBeNull()
  })

  it('sets the in-memory session on login', async () => {
    vi.mocked(authService.login).mockResolvedValue(authResponse)
    const store = useAuthStore()

    await store.login({ username: 'ada', password: 'secret123' })

    expect(store.accessToken).toBe('token-123')
    expect(store.user).toEqual(user)
    expect(store.isAuthenticated).toBe(true)
  })

  it('attemptRefresh returns true and stores the session on success', async () => {
    vi.mocked(authService.refresh).mockResolvedValue(authResponse)
    const store = useAuthStore()

    const ok = await store.attemptRefresh()

    expect(ok).toBe(true)
    expect(store.isAuthenticated).toBe(true)
  })

  it('attemptRefresh returns false and clears state on failure', async () => {
    vi.mocked(authService.refresh).mockRejectedValue(new Error('401'))
    const store = useAuthStore()

    const ok = await store.attemptRefresh()

    expect(ok).toBe(false)
    expect(store.isAuthenticated).toBe(false)
  })

  it('clears the session on logout even if the request fails', async () => {
    vi.mocked(authService.login).mockResolvedValue(authResponse)
    vi.mocked(authService.logout).mockRejectedValue(new Error('network'))
    const store = useAuthStore()
    await store.login({ username: 'ada', password: 'secret123' })

    await store.logout()

    expect(store.isAuthenticated).toBe(false)
  })

  it('bootstrap resolves once and marks the store ready', async () => {
    vi.mocked(authService.refresh).mockRejectedValue(new Error('no cookie'))
    const store = useAuthStore()

    await store.bootstrap()

    expect(store.ready).toBe(true)
    expect(authService.refresh).toHaveBeenCalledTimes(1)
  })
})
