import { beforeEach, describe, expect, it, vi } from 'vitest'

import { http } from '@/lib/http'
import { urlService } from '@/services/urlService'

vi.mock('@/lib/http', () => ({
  http: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}))

describe('urlService', () => {
  beforeEach(() => vi.clearAllMocks())

  it('creates a URL via the public endpoint (skipAuth)', () => {
    void urlService.create({ originalUrl: 'https://example.com', title: null })
    expect(http.post).toHaveBeenCalledWith(
      '/v1/urls',
      { originalUrl: 'https://example.com', title: null },
      { skipAuth: true },
    )
  })

  it('encodes the short code on lookup', () => {
    void urlService.getByCode('a/b')
    expect(http.get).toHaveBeenCalledWith('/v1/urls/a%2Fb', { skipAuth: true })
  })

  it('passes cursor and size as query params for the authed list', () => {
    void urlService.listMine({ cursor: 42, size: 20 })
    expect(http.get).toHaveBeenCalledWith('/v1/urls/my', { query: { cursor: 42, size: 20 } })
  })

  it('omits the cursor on the first page', () => {
    void urlService.listMine({ size: 20 })
    expect(http.get).toHaveBeenCalledWith('/v1/urls/my', {
      query: { cursor: undefined, size: 20 },
    })
  })

  it('targets the id-based endpoints for update and delete', () => {
    void urlService.update(7, { isActive: false })
    void urlService.remove(7)
    expect(http.put).toHaveBeenCalledWith('/v1/urls/7', { isActive: false })
    expect(http.delete).toHaveBeenCalledWith('/v1/urls/7')
  })
})
