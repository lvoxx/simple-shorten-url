<script setup lang="ts">
import { computed } from 'vue'
import { PhArrowUpRight, PhCursorClick, PhPencilSimple, PhTrash } from '@phosphor-icons/vue'

import BaseBadge from '@/components/ui/BaseBadge.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import CopyButton from '@/components/ui/CopyButton.vue'
import { formatCount, formatDate, isExpired, prettyUrl } from '@/lib/format'
import type { UrlResponse } from '@/types/api'

const props = defineProps<{ url: UrlResponse }>()
defineEmits<{ edit: [url: UrlResponse]; remove: [url: UrlResponse] }>()

const expired = computed(() => isExpired(props.url.expireAt))
const status = computed(() => {
  if (expired.value) return { tone: 'warning' as const, label: 'Expired' }
  return props.url.isActive
    ? { tone: 'success' as const, label: 'Active' }
    : { tone: 'neutral' as const, label: 'Inactive' }
})
</script>

<template>
  <BaseCard interactive class="p-4 sm:p-5">
    <div class="flex items-start justify-between gap-3">
      <div class="min-w-0 flex-1">
        <div class="flex items-center gap-2">
          <h3 class="truncate font-medium text-fg">
            {{ url.title || prettyUrl(url.shortUrl) }}
          </h3>
          <BaseBadge :tone="status.tone">{{ status.label }}</BaseBadge>
        </div>

        <div class="mt-1.5 flex items-center gap-2">
          <a
            :href="url.shortUrl"
            target="_blank"
            rel="noopener"
            class="truncate font-mono text-sm text-accent underline-offset-4 hover:underline"
          >
            {{ prettyUrl(url.shortUrl) }}
          </a>
          <a
            :href="url.shortUrl"
            target="_blank"
            rel="noopener"
            class="shrink-0 text-fg-subtle transition-colors hover:text-fg"
            aria-label="Open short link in a new tab"
          >
            <PhArrowUpRight :size="15" />
          </a>
        </div>

        <p class="mt-1 truncate text-sm text-fg-muted" :title="url.originalUrl">
          {{ prettyUrl(url.originalUrl) }}
        </p>

        <div class="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-fg-subtle">
          <span class="inline-flex items-center gap-1">
            <PhCursorClick :size="14" />
            <span class="font-mono">{{ formatCount(url.clickCount) }}</span>
            clicks
          </span>
          <span>Created {{ formatDate(url.createdAt) }}</span>
          <span v-if="url.expireAt">Expires {{ formatDate(url.expireAt) }}</span>
        </div>
      </div>
    </div>

    <div class="mt-4 flex items-center gap-2 border-t border-border/70 pt-3">
      <CopyButton :value="url.shortUrl" label="Copy" size="sm" />
      <button
        type="button"
        class="inline-flex h-9 items-center gap-1.5 rounded-[--radius-input] px-3 text-sm font-medium text-fg-muted transition-colors hover:bg-surface-2 hover:text-fg active:scale-[0.98]"
        @click="$emit('edit', url)"
      >
        <PhPencilSimple :size="16" />
        Edit
      </button>
      <button
        type="button"
        class="ml-auto inline-flex h-9 items-center gap-1.5 rounded-[--radius-input] px-3 text-sm font-medium text-fg-muted transition-colors hover:bg-danger/12 hover:text-danger active:scale-[0.98]"
        @click="$emit('remove', url)"
      >
        <PhTrash :size="16" />
        Delete
      </button>
    </div>
  </BaseCard>
</template>
