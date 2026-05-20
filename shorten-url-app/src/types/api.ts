export const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'

export const API_PATHS = {
  AUTH: {
    REGISTER: '/api/v1/auth/register',
    LOGIN: '/api/v1/auth/login',
    REFRESH: '/api/v1/auth/refresh',
    LOGOUT: '/api/v1/auth/logout',
  },
  USERS: {
    ME: '/api/v1/users/me',
    UPDATE_EMAIL: '/api/v1/users/me/email',
    DEACTIVATE: '/api/v1/users/me',
  },
  URLS: {
    CREATE: '/api/v1/urls',
    BY_SHORT_CODE: (code: string) => `/api/v1/urls/${code}`,
    MY_URLS: '/api/v1/urls/my',
    UPDATE: (id: number) => `/api/v1/urls/${id}`,
    DELETE: (id: number) => `/api/v1/urls/${id}`,
  },
} as const
