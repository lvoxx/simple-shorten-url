import { ApiService } from './ApiService'
import type { CreateUrlRequest, CursorPage, UpdateUrlRequest, Url } from '../types'

export class UrlService {
  constructor(private api: ApiService) {}

  async createUrl(data: CreateUrlRequest): Promise<Url> {
    return this.api.post<Url>('/api/v1/urls', data)
  }

  async getByShortCode(shortCode: string): Promise<Url> {
    return this.api.get<Url>(`/api/v1/urls/${shortCode}`)
  }

  async listMyUrls(cursor?: number, size: number = 20): Promise<CursorPage<Url>> {
    return this.api.get<CursorPage<Url>>('/api/v1/urls/my', {
      params: { cursor, size },
    })
  }

  async updateUrl(id: number, data: UpdateUrlRequest): Promise<Url> {
    return this.api.put<Url>(`/api/v1/urls/${id}`, data)
  }

  async deleteUrl(id: number): Promise<void> {
    await this.api.delete(`/api/v1/urls/${id}`)
  }
}
