import { http } from '@/lib/http'
import type {
  CreateUrlRequest,
  CursorPage,
  UpdateUrlRequest,
  UrlResponse,
} from '@/types/api'

/** URL endpoints (/api/v1/urls). Create + lookup are public; the rest require auth. */
export const urlService = {
  create(payload: CreateUrlRequest): Promise<UrlResponse> {
    return http.post<UrlResponse>('/v1/urls', payload, { skipAuth: true })
  },

  getByCode(shortCode: string): Promise<UrlResponse> {
    return http.get<UrlResponse>(`/v1/urls/${encodeURIComponent(shortCode)}`, { skipAuth: true })
  },

  listMine(params: { cursor?: number | null; size?: number } = {}): Promise<CursorPage<UrlResponse>> {
    return http.get<CursorPage<UrlResponse>>('/v1/urls/my', {
      query: { cursor: params.cursor ?? undefined, size: params.size ?? undefined },
    })
  },

  update(id: number, payload: UpdateUrlRequest): Promise<UrlResponse> {
    return http.put<UrlResponse>(`/v1/urls/${id}`, payload)
  },

  remove(id: number): Promise<void> {
    return http.delete<void>(`/v1/urls/${id}`)
  },
}
