import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import UrlCard from '@/components/urls/UrlCard.vue'
import type { UrlResponse } from '@/types/api'

function makeUrl(overrides: Partial<UrlResponse> = {}): UrlResponse {
  return {
    id: 1,
    shortCode: 'abc123',
    shortUrl: 'http://localhost:8081/abc123',
    originalUrl: 'https://example.com/some/long/path',
    title: 'My link',
    isActive: true,
    clickCount: 1500,
    expireAt: null,
    createdAt: '2026-01-01T00:00:00',
    ...overrides,
  }
}

describe('UrlCard', () => {
  it('renders the title, short link, and compact click count', () => {
    const wrapper = mount(UrlCard, { props: { url: makeUrl() } })
    expect(wrapper.text()).toContain('My link')
    expect(wrapper.text()).toContain('localhost:8081/abc123')
    expect(wrapper.text()).toContain('1.5K')
  })

  it('shows an Active badge for active, non-expired links', () => {
    const wrapper = mount(UrlCard, { props: { url: makeUrl() } })
    expect(wrapper.text()).toContain('Active')
  })

  it('shows Inactive when the link is disabled', () => {
    const wrapper = mount(UrlCard, { props: { url: makeUrl({ isActive: false }) } })
    expect(wrapper.text()).toContain('Inactive')
  })

  it('shows Expired when the expiry is in the past, regardless of active flag', () => {
    const wrapper = mount(UrlCard, {
      props: { url: makeUrl({ isActive: true, expireAt: '2000-01-01T00:00:00' }) },
    })
    expect(wrapper.text()).toContain('Expired')
  })

  it('emits edit and remove with the url', async () => {
    const url = makeUrl()
    const wrapper = mount(UrlCard, { props: { url } })
    const buttons = wrapper.findAll('button')
    const editBtn = buttons.find((b) => b.text().includes('Edit'))
    const deleteBtn = buttons.find((b) => b.text().includes('Delete'))

    await editBtn?.trigger('click')
    await deleteBtn?.trigger('click')

    expect(wrapper.emitted('edit')?.[0]).toEqual([url])
    expect(wrapper.emitted('remove')?.[0]).toEqual([url])
  })
})
