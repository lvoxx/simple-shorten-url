<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  PhBroadcast,
  PhCursorClick,
  PhLink,
  PhTrendDown,
  PhTrendUp,
  PhUsers,
} from '@phosphor-icons/vue'

import MiniBarChart, { type BarItem } from '@/components/charts/MiniBarChart.vue'
import MiniLineChart from '@/components/charts/MiniLineChart.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseSkeleton from '@/components/ui/BaseSkeleton.vue'
import { useDashboardSocket } from '@/composables/useDashboardSocket'
import { formatCount } from '@/lib/format'
import { dashboardService, type DashboardRange } from '@/services/dashboardService'
import { useToastStore } from '@/stores/toast'
import type {
  DashboardLiveTick,
  DashboardOverview,
  TimeSeriesPoint,
  TopItem,
} from '@/types/api'

const toast = useToastStore()

const RANGES: { label: string; value: DashboardRange }[] = [
  { label: '7 days', value: '7d' },
  { label: '30 days', value: '30d' },
  { label: '90 days', value: '90d' },
]
const range = ref<DashboardRange>('7d')

const loading = ref(true)
const overview = ref<DashboardOverview | null>(null)
const series = ref<TimeSeriesPoint[]>([])
const topLinks = ref<TopItem[]>([])
const topReferers = ref<TopItem[]>([])

const seriesValues = computed(() => series.value.map((p) => p.clicks))
const seriesLabels = computed(() => series.value.map((p) => p.date))
const linkBars = computed<BarItem[]>(() =>
  topLinks.value.map((t) => ({ label: t.label, value: t.clicks, hint: t.key })),
)
const refererBars = computed<BarItem[]>(() =>
  topReferers.value.map((t) => ({ label: t.label, value: t.clicks })),
)

const trendUp = computed(() => (overview.value?.trendPct ?? 0) >= 0)

async function load(): Promise<void> {
  loading.value = true
  try {
    const [ov, ts, links, referers] = await Promise.all([
      dashboardService.overview(range.value),
      dashboardService.timeseries(range.value),
      dashboardService.topLinks(range.value, 8),
      dashboardService.topReferers(range.value, 8),
    ])
    overview.value = ov
    series.value = ts
    topLinks.value = links
    topReferers.value = referers
  } catch {
    toast.error('Could not load analytics', 'Please try again in a moment.')
  } finally {
    loading.value = false
  }
}

function selectRange(value: DashboardRange): void {
  if (value === range.value) return
  range.value = value
  void load()
}

// ── Live updates ────────────────────────────────────────────────────────────
const liveActive = ref(false)
let pulseTimer: ReturnType<typeof setTimeout> | null = null

function onTick(tick: DashboardLiveTick): void {
  // Reflect the click immediately in the headline + top list, optimistically.
  if (overview.value) {
    overview.value = {
      ...overview.value,
      totalClicks: overview.value.totalClicks + 1,
      clicksToday: overview.value.clicksToday + 1,
    }
  }
  const link = topLinks.value.find((l) => l.key === tick.shortCode)
  if (link) link.clicks += 1
  // Bump today's bucket in the series (last point) if present.
  const last = series.value.at(-1)
  if (last) last.clicks += 1

  // Brief visual pulse on the live badge.
  liveActive.value = true
  if (pulseTimer) clearTimeout(pulseTimer)
  pulseTimer = setTimeout(() => (liveActive.value = false), 1200)
}

const { status, open, close } = useDashboardSocket(onTick)
const connected = computed(() => status.value === 'OPEN')

onMounted(() => {
  void load()
  open()
})

onBeforeUnmount(() => {
  if (pulseTimer) clearTimeout(pulseTimer)
  close()
})
</script>

<template>
  <div class="mx-auto max-w-5xl px-4 py-10 sm:px-6 sm:py-14">
    <header class="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight text-fg">Analytics</h1>
        <p class="mt-1 text-sm text-fg-muted">Clicks, visitors, and top links across your URLs.</p>
      </div>

      <div class="flex items-center gap-3">
        <span
          class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium transition-colors"
          :class="connected ? 'bg-success/10 text-success' : 'bg-border/60 text-fg-subtle'"
          :title="connected ? 'Live updates connected' : 'Live updates offline'"
        >
          <PhBroadcast :size="14" :weight="liveActive ? 'fill' : 'regular'" />
          {{ connected ? 'Live' : 'Offline' }}
        </span>

        <div class="inline-flex rounded-[--radius-input] border border-border p-0.5" role="tablist">
          <button
            v-for="r in RANGES"
            :key="r.value"
            type="button"
            class="rounded-[calc(var(--radius-input)-2px)] px-3 py-1.5 text-sm font-medium transition-colors"
            :class="
              range === r.value ? 'bg-accent text-accent-fg' : 'text-fg-muted hover:text-fg'
            "
            @click="selectRange(r.value)"
          >
            {{ r.label }}
          </button>
        </div>
      </div>
    </header>

    <!-- Overview cards -->
    <div class="grid grid-cols-2 gap-4 lg:grid-cols-4">
      <BaseCard class="p-5">
        <div class="flex items-center gap-2 text-fg-subtle">
          <PhCursorClick :size="18" /><span class="text-xs font-medium uppercase tracking-wide">Total clicks</span>
        </div>
        <BaseSkeleton v-if="loading" class="mt-3" height="2rem" width="5rem" />
        <p v-else class="mt-2 text-3xl font-semibold text-fg">{{ formatCount(overview?.totalClicks ?? 0) }}</p>
        <p v-if="!loading" class="mt-1 inline-flex items-center gap-1 text-xs"
          :class="trendUp ? 'text-success' : 'text-danger'">
          <component :is="trendUp ? PhTrendUp : PhTrendDown" :size="14" />
          {{ Math.abs(overview?.trendPct ?? 0) }}% vs prev.
        </p>
      </BaseCard>

      <BaseCard class="p-5">
        <div class="flex items-center gap-2 text-fg-subtle">
          <PhUsers :size="18" /><span class="text-xs font-medium uppercase tracking-wide">Unique visitors</span>
        </div>
        <BaseSkeleton v-if="loading" class="mt-3" height="2rem" width="5rem" />
        <p v-else class="mt-2 text-3xl font-semibold text-fg">{{ formatCount(overview?.uniqueVisitors ?? 0) }}</p>
      </BaseCard>

      <BaseCard class="p-5">
        <div class="flex items-center gap-2 text-fg-subtle">
          <PhCursorClick :size="18" /><span class="text-xs font-medium uppercase tracking-wide">Clicks today</span>
        </div>
        <BaseSkeleton v-if="loading" class="mt-3" height="2rem" width="5rem" />
        <p v-else class="mt-2 text-3xl font-semibold text-fg">{{ formatCount(overview?.clicksToday ?? 0) }}</p>
      </BaseCard>

      <BaseCard class="p-5">
        <div class="flex items-center gap-2 text-fg-subtle">
          <PhLink :size="18" /><span class="text-xs font-medium uppercase tracking-wide">Active links</span>
        </div>
        <BaseSkeleton v-if="loading" class="mt-3" height="2rem" width="5rem" />
        <p v-else class="mt-2 text-3xl font-semibold text-fg">{{ formatCount(overview?.activeLinks ?? 0) }}</p>
      </BaseCard>
    </div>

    <!-- Time series -->
    <BaseCard class="mt-6 p-5 sm:p-6">
      <h2 class="mb-4 font-semibold text-fg">Clicks over time</h2>
      <BaseSkeleton v-if="loading" height="120px" />
      <MiniLineChart v-else :values="seriesValues" :labels="seriesLabels" :height="120" />
    </BaseCard>

    <!-- Top lists -->
    <div class="mt-6 grid gap-4 md:grid-cols-2">
      <BaseCard class="p-5 sm:p-6">
        <h2 class="mb-4 font-semibold text-fg">Top links</h2>
        <BaseSkeleton v-if="loading" height="10rem" />
        <MiniBarChart v-else :items="linkBars" />
      </BaseCard>

      <BaseCard class="p-5 sm:p-6">
        <h2 class="mb-4 font-semibold text-fg">Top referers</h2>
        <BaseSkeleton v-if="loading" height="10rem" />
        <MiniBarChart v-else :items="refererBars" />
      </BaseCard>
    </div>
  </div>
</template>
