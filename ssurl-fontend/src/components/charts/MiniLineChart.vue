<script setup lang="ts">
import { computed } from 'vue'

/**
 * Dependency-free SVG line/area chart for a daily click series. Scales to its
 * container width via a non-uniform viewBox; height is fixed by `height`.
 */
const props = withDefaults(
  defineProps<{
    values: number[]
    /** Pixel height of the chart area. */
    height?: number
    /** Optional ISO date labels aligned with `values` (for the x-axis ticks). */
    labels?: string[]
  }>(),
  { height: 120, labels: () => [] },
)

const W = 600 // viewBox width units; SVG stretches to container width
const PAD = 6

const max = computed(() => Math.max(1, ...props.values))

const points = computed(() => {
  const n = props.values.length
  if (n === 0) return ''
  const h = props.height
  const stepX = n > 1 ? (W - PAD * 2) / (n - 1) : 0
  return props.values
    .map((v, i) => {
      const x = PAD + i * stepX
      const y = h - PAD - (v / max.value) * (h - PAD * 2)
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
})

/** Closed path (line + baseline) used for the gradient fill under the curve. */
const areaPath = computed(() => {
  const coords = points.value ? points.value.split(' ') : []
  if (coords.length === 0) return ''
  const xFirst = coords[0]?.split(',')[0] ?? '0'
  const xLast = coords.at(-1)?.split(',')[0] ?? xFirst
  const baseline = props.height - PAD
  return `M ${xFirst},${baseline} L ${points.value.replaceAll(' ', ' L ')} L ${xLast},${baseline} Z`
})

const empty = computed(() => props.values.every((v) => v === 0))
</script>

<template>
  <div class="w-full">
    <svg
      :viewBox="`0 0 ${W} ${height}`"
      :height="height"
      preserveAspectRatio="none"
      class="w-full overflow-visible"
      role="img"
      aria-label="Clicks over time"
    >
      <defs>
        <linearGradient id="dash-line-fill" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="var(--color-accent, #6366f1)" stop-opacity="0.25" />
          <stop offset="100%" stop-color="var(--color-accent, #6366f1)" stop-opacity="0" />
        </linearGradient>
      </defs>

      <template v-if="!empty">
        <path :d="areaPath" fill="url(#dash-line-fill)" />
        <polyline
          :points="points"
          fill="none"
          stroke="var(--color-accent, #6366f1)"
          stroke-width="2"
          stroke-linejoin="round"
          stroke-linecap="round"
          vector-effect="non-scaling-stroke"
        />
      </template>
      <line
        v-else
        :x1="PAD"
        :x2="W - PAD"
        :y1="height / 2"
        :y2="height / 2"
        stroke="var(--color-border, #e5e7eb)"
        stroke-dasharray="4 4"
        vector-effect="non-scaling-stroke"
      />
    </svg>
    <p v-if="empty" class="mt-2 text-center text-xs text-fg-subtle">No clicks in this range yet</p>
  </div>
</template>
