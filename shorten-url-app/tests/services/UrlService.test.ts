import { describe, it, expect, vi, beforeEach } from 'vitest'
import { UrlService } from '../../shared/services/UrlService'
import { ApiService } from '../../shared/services/ApiService'
import type { Url, CursorPage, CreateUrlRequest, UpdateUrlRequest } from '../../shared/types'

describe('UrlService', () => {
  let api: ApiService
  let urlService: UrlService

  const mockUrl: Url = {
    id: 1,
    shortCode: 'abc123',
    shortUrl: 'http://short.url/abc123',
    originalUrl: 'https://example.com/long-url',
    title: 'Example',
    isActive: true,
    clickCount: 0,
    expireAt: null,
    createdAt: '2026-05-14T12:00:00',
  }

  const mockCursorPage: CursorPage<Url> = {
    content: [mockUrl],
    nextCursor: null,
    hasNext: false,
  }

  beforeEach(() => {
    api = new ApiService()
    urlService = new UrlService(api)
  })

  describe('createUrl', () => {
    it('should POST and return created Url', async () => {
      vi.spyOn(api, 'post').mockResolvedValue(mockUrl)

      const request: CreateUrlRequest = {
        originalUrl: 'https://example.com/long-url',
        title: 'Example',
      }
      const result = await urlService.createUrl(request)

      expect(api.post).toHaveBeenCalledWith('/api/v1/urls', request)
      expect(result).toEqual(mockUrl)
    })

    it('should create URL without optional fields', async () => {
      vi.spyOn(api, 'post').mockResolvedValue(mockUrl)

      const request: CreateUrlRequest = {
        originalUrl: 'https://example.com/other',
      }
      await urlService.createUrl(request)

      expect(api.post).toHaveBeenCalledWith('/api/v1/urls', request)
    })
  })

  describe('getByShortCode', () => {
    it('should GET URL by short code', async () => {
      vi.spyOn(api, 'get').mockResolvedValue(mockUrl)

      const result = await urlService.getByShortCode('abc123')

      expect(api.get).toHaveBeenCalledWith('/api/v1/urls/abc123')
      expect(result).toEqual(mockUrl)
    })
  })

  describe('listMyUrls', () => {
    it('should GET paginated URLs without cursor', async () => {
      vi.spyOn(api, 'get').mockResolvedValue(mockCursorPage)

      const result = await urlService.listMyUrls()

      expect(api.get).toHaveBeenCalledWith('/api/v1/urls/my', { params: { cursor: undefined, size: 20 } })
      expect(result).toEqual(mockCursorPage)
    })

    it('should GET paginated URLs with cursor', async () => {
      vi.spyOn(api, 'get').mockResolvedValue(mockCursorPage)

      const result = await urlService.listMyUrls(10, 5)

      expect(api.get).toHaveBeenCalledWith('/api/v1/urls/my', { params: { cursor: 10, size: 5 } })
      expect(result).toEqual(mockCursorPage)
    })
  })

  describe('updateUrl', () => {
    it('should PUT and return updated Url', async () => {
      vi.spyOn(api, 'put').mockResolvedValue(mockUrl)

      const request: UpdateUrlRequest = { title: 'Updated Title' }
      const result = await urlService.updateUrl(1, request)

      expect(api.put).toHaveBeenCalledWith('/api/v1/urls/1', request)
      expect(result).toEqual(mockUrl)
    })
  })

  describe('deleteUrl', () => {
    it('should DELETE url by id', async () => {
      vi.spyOn(api, 'delete').mockResolvedValue(undefined)

      await urlService.deleteUrl(1)

      expect(api.delete).toHaveBeenCalledWith('/api/v1/urls/1')
    })
  })
})
