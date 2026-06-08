/**
 * Thin fetch wrapper around the backend REST API.
 *
 * Responsibilities:
 *  - prefix every request with the API base (`/api`, proxied to api_service)
 *  - send/receive JSON and always include credentials (refresh cookie)
 *  - attach the in-memory access token as a Bearer header
 *  - on 401, transparently attempt ONE refresh and retry the request once
 *  - parse ProblemDetail bodies into a typed ApiError
 *
 * The auth concerns are injected via `configureHttp` so this module stays
 * decoupled from the Pinia store (avoids a circular import).
 */
import { ApiError, normalizeNetworkError, normalizeProblem, type ProblemDetail } from './problem'

const API_BASE = import.meta.env.VITE_API_BASE ?? '/api'

type TokenProvider = () => string | null
type RefreshHandler = () => Promise<boolean>
type UnauthorizedHandler = () => void

interface AuthBridge {
  getToken: TokenProvider
  refresh: RefreshHandler
  onUnauthorized: UnauthorizedHandler
}

let bridge: AuthBridge = {
  getToken: () => null,
  refresh: async () => false,
  onUnauthorized: () => {},
}

/** Wire the access-token getter, refresh handler, and logout callback. */
export function configureHttp(partial: Partial<AuthBridge>): void {
  bridge = { ...bridge, ...partial }
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  /** JSON-serializable request body. */
  body?: unknown
  /** Query parameters; null/undefined values are skipped. */
  query?: Record<string, string | number | boolean | null | undefined>
  /** Skip the Bearer header (used by auth endpoints that rely on the cookie). */
  skipAuth?: boolean
  /** Skip the 401 → refresh → retry flow (used by the refresh call itself). */
  skipRefresh?: boolean
  signal?: AbortSignal
}

function buildUrl(path: string, query?: RequestOptions['query']): string {
  const url = `${API_BASE}${path}`
  if (!query) return url
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value !== null && value !== undefined) params.set(key, String(value))
  }
  const qs = params.toString()
  return qs ? `${url}?${qs}` : url
}

async function parseBody(response: Response): Promise<unknown> {
  if (response.status === 204) return undefined
  const text = await response.text()
  if (!text) return undefined
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

async function rawRequest(path: string, options: RequestOptions): Promise<Response> {
  const headers: Record<string, string> = { Accept: 'application/json' }

  if (options.body !== undefined) headers['Content-Type'] = 'application/json'

  if (!options.skipAuth) {
    const token = bridge.getToken()
    if (token) headers.Authorization = `Bearer ${token}`
  }

  return fetch(buildUrl(path, options.query), {
    method: options.method ?? 'GET',
    headers,
    credentials: 'include',
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    signal: options.signal,
  })
}

/**
 * Perform a request and return the parsed JSON body typed as `T`.
 * Throws `ApiError` for non-2xx responses (or network failures).
 */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  let response: Response
  try {
    response = await rawRequest(path, options)
  } catch (error) {
    throw new ApiError(normalizeNetworkError(error))
  }

  // Transparent refresh-and-retry on a single 401.
  if (response.status === 401 && !options.skipRefresh && !options.skipAuth) {
    const refreshed = await bridge.refresh()
    if (refreshed) {
      try {
        response = await rawRequest(path, options)
      } catch (error) {
        throw new ApiError(normalizeNetworkError(error))
      }
    } else {
      bridge.onUnauthorized()
    }
  }

  if (!response.ok) {
    const body = (await parseBody(response)) as ProblemDetail | undefined
    throw new ApiError(normalizeProblem(response.status, body))
  }

  return (await parseBody(response)) as T
}

export const http = {
  get: <T>(path: string, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    request<T>(path, { ...options, method: 'GET' }),
  post: <T>(path: string, body?: unknown, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    request<T>(path, { ...options, method: 'POST', body }),
  put: <T>(path: string, body?: unknown, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    request<T>(path, { ...options, method: 'PUT', body }),
  delete: <T>(path: string, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    request<T>(path, { ...options, method: 'DELETE' }),
}
