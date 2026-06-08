/**
 * RFC-7807 ProblemDetail handling. The backend's GlobalExceptionHandler returns
 * `{ type, title, status, detail, instance, errors? }` where `errors` is a list
 * of `"field: message"` strings on 400 validation failures.
 */

export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  errors?: string[]
}

/** Normalized, UI-friendly shape consumed by stores, composables, and views. */
export interface NormalizedError {
  status: number
  title: string
  /** Human-readable message safe to surface in a toast or banner. */
  message: string
  /** Per-field messages parsed from the `errors[]` array, keyed by field name. */
  fieldErrors: Record<string, string>
  /** The raw problem detail, when available. */
  problem?: ProblemDetail
}

/** Thrown by the HTTP client for any non-2xx response. */
export class ApiError extends Error {
  readonly status: number
  readonly title: string
  readonly fieldErrors: Record<string, string>
  readonly problem?: ProblemDetail

  constructor(normalized: NormalizedError) {
    super(normalized.message)
    this.name = 'ApiError'
    this.status = normalized.status
    this.title = normalized.title
    this.fieldErrors = normalized.fieldErrors
    this.problem = normalized.problem
  }
}

/** Parse a `"field: message"` entry into a `[field, message]` pair. */
function parseFieldError(entry: string): [string, string] | null {
  const idx = entry.indexOf(':')
  if (idx <= 0) return null
  const field = entry.slice(0, idx).trim()
  const message = entry.slice(idx + 1).trim()
  if (!field || !message) return null
  return [field, message]
}

/** Build a NormalizedError from a parsed ProblemDetail body and HTTP status. */
export function normalizeProblem(status: number, problem?: ProblemDetail): NormalizedError {
  const fieldErrors: Record<string, string> = {}
  for (const entry of problem?.errors ?? []) {
    const parsed = parseFieldError(entry)
    if (parsed) fieldErrors[parsed[0]] = parsed[1]
  }

  const title = problem?.title?.trim() || defaultTitle(status)
  const message = problem?.detail?.trim() || title

  return { status, title, message, fieldErrors, problem }
}

/** Network-level failure (no HTTP response) normalized to a stable shape. */
export function normalizeNetworkError(error: unknown): NormalizedError {
  const message =
    error instanceof Error && error.message
      ? `Could not reach the server. ${error.message}`
      : 'Could not reach the server. Check your connection and try again.'
  return { status: 0, title: 'Network error', message, fieldErrors: {} }
}

function defaultTitle(status: number): string {
  switch (status) {
    case 0:
      return 'Network error'
    case 400:
      return 'Invalid request'
    case 401:
      return 'Authentication required'
    case 403:
      return 'Not allowed'
    case 404:
      return 'Not found'
    case 409:
      return 'Already exists'
    case 410:
      return 'No longer available'
    case 422:
      return 'Cannot process request'
    case 429:
      return 'Too many requests'
    default:
      return status >= 500 ? 'Server error' : 'Request failed'
  }
}

/** Convenience guard for consumers catching errors from the service layer. */
export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}
