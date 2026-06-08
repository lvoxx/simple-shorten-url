import { describe, expect, it } from 'vitest'

import { ApiError, isApiError, normalizeProblem } from '@/lib/problem'

describe('normalizeProblem', () => {
  it('parses the errors[] array into a field-keyed map', () => {
    const normalized = normalizeProblem(400, {
      title: 'Validation Failed',
      detail: 'Request validation failed',
      errors: ['username: size must be between 3 and 50', 'email: must be a valid email address'],
    })

    expect(normalized.status).toBe(400)
    expect(normalized.fieldErrors).toEqual({
      username: 'size must be between 3 and 50',
      email: 'must be a valid email address',
    })
    expect(normalized.message).toBe('Request validation failed')
  })

  it('falls back to a status-derived title when none is provided', () => {
    const normalized = normalizeProblem(429, undefined)
    expect(normalized.title).toBe('Too many requests')
    expect(normalized.message).toBe('Too many requests')
    expect(normalized.fieldErrors).toEqual({})
  })

  it('ignores malformed field-error entries', () => {
    const normalized = normalizeProblem(400, { errors: ['no-colon-here', ': empty field'] })
    expect(normalized.fieldErrors).toEqual({})
  })
})

describe('ApiError', () => {
  it('exposes status, title, and field errors and is detectable via guard', () => {
    const error = new ApiError(normalizeProblem(409, { title: 'Conflict', detail: 'User exists' }))
    expect(error).toBeInstanceOf(Error)
    expect(isApiError(error)).toBe(true)
    expect(error.status).toBe(409)
    expect(error.message).toBe('User exists')
  })
})
