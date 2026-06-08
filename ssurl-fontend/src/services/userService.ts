import { http } from '@/lib/http'
import type { UserResponse } from '@/types/api'

/** Current-user account endpoints (/api/v1/users/me). */
export const userService = {
  updateEmail(email: string): Promise<UserResponse> {
    return http.put<UserResponse>('/v1/users/me/email', undefined, { query: { email } })
  },

  deleteAccount(): Promise<void> {
    return http.delete<void>('/v1/users/me')
  },
}
