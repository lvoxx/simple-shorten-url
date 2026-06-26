import { http } from '@/lib/http'
import type {
  CodeStats,
  DashboardOverview,
  TimeSeriesPoint,
  TopItem,
} from '@/types/api'

/** Time-range tokens accepted by the dashboard endpoints. */
export type DashboardRange = '24h' | '7d' | '14d' | '30d' | '90d'

/**
 * Dashboard analytics endpoints (/api/v1/dashboard/*). All require auth — the
 * shared `http` client attaches the Bearer token and handles refresh/retry.
 * Requests are proxied to the dashboard service (port 8083), not api_service.
 */
export const dashboardService = {
  overview(range: DashboardRange = '7d'): Promise<DashboardOverview> {
    return http.get<DashboardOverview>('/v1/dashboard/overview', { query: { range } })
  },

  timeseries(range: DashboardRange = '30d', code?: string): Promise<TimeSeriesPoint[]> {
    return http.get<TimeSeriesPoint[]>('/v1/dashboard/timeseries', {
      query: { range, code: code ?? undefined },
    })
  },

  topLinks(range: DashboardRange = '30d', limit = 10): Promise<TopItem[]> {
    return http.get<TopItem[]>('/v1/dashboard/top/links', { query: { range, limit } })
  },

  topReferers(range: DashboardRange = '30d', limit = 10): Promise<TopItem[]> {
    return http.get<TopItem[]>('/v1/dashboard/top/referers', { query: { range, limit } })
  },

  linkStats(shortCode: string, range: DashboardRange = '30d'): Promise<CodeStats> {
    return http.get<CodeStats>(`/v1/dashboard/links/${encodeURIComponent(shortCode)}`, {
      query: { range },
    })
  },
}
