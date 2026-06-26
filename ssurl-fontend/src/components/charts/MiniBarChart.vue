<script setup lang="ts">
import { computed } from 'vue'

import { formatCount } from '@/lib/format'

export interface BarItem {
  label: string
  value: number
  /** Optional sublabel (e.g. short code under a title). */
  hint?: string
}

/**
 * Horizontal proportional bars — dependency-free. Doubles as the "top N" list:
 * each row shows a label, a bar sized relative to the largest value, and a count.
 */
const props = defineProps<{ items: BarItem[] }>()

const max = computed(() => Math.max(1, ...props.items.map((i) => i.value)))
function widthPct(value: number): string {
  return `${Math.max(2, Math.round((value / max.value) * 100))}%`
}
</script>

<template>
  <ul v-if="items.length" class="flex flex-col gap-3">
    <li v-for="(item, i) in items" :key="`${item.label}-${i}`" class="text-sm">
      <div class="mb-1 flex items-baseline justify-between gap-3">
        <span class="min-w-0 truncate text-fg" :title="item.label">{{ item.label }}</span>
        <span class="shrink-0 font-mono text-xs text-fg-muted">{{ formatCount(item.value) }}</span>
      </div>
      <div class="h-2 w-full overflow-hidden rounded-full bg-border/60">
        <div class="h-full rounded-full bg-accent" :style="{ width: widthPct(item.value) }" />
      </div>
      <p v-if="item.hint" class="mt-0.5 truncate font-mono text-xs text-fg-subtle">{{ item.hint }}</p>
    </li>
  </ul>
  <p v-else class="text-sm text-fg-subtle">No data in this range yet.</p>
</template>
