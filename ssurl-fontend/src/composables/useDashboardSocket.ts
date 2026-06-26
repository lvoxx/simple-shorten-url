import { ref } from 'vue'
import { useWebSocket } from '@vueuse/core'

import { useAuthStore } from '@/stores/auth'
import type { DashboardLiveTick } from '@/types/api'

/**
 * Subscribes to the dashboard's live click stream (`/ws/dashboard`).
 *
 * The browser WebSocket API can't send the Authorization header, so the access
 * token is passed as a query param (validated on handshake by the dashboard
 * service). Ticks are global; callers filter to the codes they display.
 *
 * Connection is not started automatically — call `open()` once auth is ready.
 */
export function useDashboardSocket(onTick?: (tick: DashboardLiveTick) => void) {
  const auth = useAuthStore()
  const lastTick = ref<DashboardLiveTick | null>(null)

  function buildUrl(): string {
    const proto = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const token = auth.accessToken ?? ''
    return `${proto}://${window.location.host}/ws/dashboard?token=${encodeURIComponent(token)}`
  }

  const { status, open, close } = useWebSocket(buildUrl, {
    immediate: false,
    autoReconnect: { retries: 5, delay: 2000 },
    onMessage(_ws, event) {
      try {
        const tick = JSON.parse(event.data as string) as DashboardLiveTick
        lastTick.value = tick
        onTick?.(tick)
      } catch {
        // Ignore malformed frames.
      }
    },
  })

  return { status, lastTick, open, close }
}
