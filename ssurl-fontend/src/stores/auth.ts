import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { authService } from '@/services/authService'
import type { LoginRequest, RegisterRequest, UserResponse } from '@/types/api'

/**
 * Authentication state.
 *
 * The access token lives ONLY in memory (matches the backend's design — access
 * tokens are never persisted client-side). The refresh token is an HTTP-only
 * cookie the browser manages, so a session is rehydrated on reload via
 * `bootstrap()` → silent refresh.
 */
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(null)
  const user = ref<UserResponse | null>(null)
  /** True once the initial silent-refresh attempt has resolved. */
  const ready = ref(false)

  const isAuthenticated = computed(() => accessToken.value !== null && user.value !== null)

  function setSession(token: string, currentUser: UserResponse): void {
    accessToken.value = token
    user.value = currentUser
  }

  function clearSession(): void {
    accessToken.value = null
    user.value = null
  }

  async function login(payload: LoginRequest): Promise<void> {
    const res = await authService.login(payload)
    setSession(res.accessToken, res.user)
  }

  async function register(payload: RegisterRequest): Promise<void> {
    await authService.register(payload)
    // Backend register returns the user only (no tokens); log in to start a session.
    await login({ username: payload.username, password: payload.password })
  }

  /**
   * Exchange the refresh cookie for a fresh access token.
   * Returns true on success. Used both by `bootstrap()` and the HTTP 401 retry.
   */
  async function attemptRefresh(): Promise<boolean> {
    try {
      const res = await authService.refresh()
      setSession(res.accessToken, res.user)
      return true
    } catch {
      clearSession()
      return false
    }
  }

  /** Restore a session on app load; always resolves (never throws). */
  async function bootstrap(): Promise<void> {
    if (ready.value) return
    await attemptRefresh()
    ready.value = true
  }

  async function logout(): Promise<void> {
    try {
      await authService.logout()
    } finally {
      clearSession()
    }
  }

  return {
    accessToken,
    user,
    ready,
    isAuthenticated,
    login,
    register,
    attemptRefresh,
    bootstrap,
    logout,
    clearSession,
  }
})
