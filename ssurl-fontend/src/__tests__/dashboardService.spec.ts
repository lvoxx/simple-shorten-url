import { beforeEach, describe, expect, it, vi } from 'vitest'

import { http } from '@/lib/http'
import { dashboardService } from '@/services/dashboardService'

vi.mock('@/lib/http', () => ({
  http: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}))

describe('dashboardService', () => {
  beforeEach(() => vi.clearAllMocks())

  it('requests the overview with the selected range', () => {
    void dashboardService.overview('30d')
    expect(http.get).toHaveBeenCalledWith('/v1/dashboard/overview', { query: { range: '30d' } })
  })

  it('defaults the overview range to 7d', () => {
    void dashboardService.overview()
    expect(http.get).toHaveBeenCalledWith('/v1/dashboard/overview', { query: { range: '7d' } })
  })

  it('passes an optional code to the timeseries endpoint', () => {
    void dashboardService.timeseries('90d', 'abc123')
    expect(http.get).toHaveBeenCalledWith('/v1/dashboard/timeseries', {
      query: { range: '90d', code: 'abc123' },
    })
  })

  it('omits the code when not provided', () => {
    void dashboardService.timeseries('7d')
    expect(http.get).toHaveBeenCalledWith('/v1/dashboard/timeseries', {
      query: { range: '7d', code: undefined },
    })
  })

  it('passes range and limit for top links and referers', () => {
    void dashboardService.topLinks('30d', 8)
    void dashboardService.topReferers('30d', 5)
    expect(http.get).toHaveBeenCalledWith('/v1/dashboard/top/links', { query: { range: '30d', limit: 8 } })
    expect(http.get).toHaveBeenCalledWith('/v1/dashboard/top/referers', { query: { range: '30d', limit: 5 } })
  })

  it('encodes the short code for per-link stats', () => {
    void dashboardService.linkStats('a/b', '30d')
    expect(http.get).toHaveBeenCalledWith('/v1/dashboard/links/a%2Fb', { query: { range: '30d' } })
  })
})
