import { describe, expect, it } from 'vitest'

import { formatCount, formatDate, isExpired, prettyUrl } from '@/lib/format'

describe('format helpers', () => {
  it('formats large counts compactly', () => {
    expect(formatCount(0)).toBe('0')
    expect(formatCount(1500)).toBe('1.5K')
  })

  it('returns an em-dash-free placeholder for null dates', () => {
    expect(formatDate(null)).toBe('—')
    expect(formatDate('not-a-date')).toBe('—')
  })

  it('detects expiry relative to now', () => {
    expect(isExpired('2000-01-01T00:00:00')).toBe(true)
    expect(isExpired('2999-01-01T00:00:00')).toBe(false)
    expect(isExpired(null)).toBe(false)
  })

  it('strips scheme and trailing slash for display', () => {
    expect(prettyUrl('https://example.com/path/')).toBe('example.com/path')
    expect(prettyUrl('http://EXAMPLE.com')).toBe('EXAMPLE.com')
  })
})
