<script setup lang="ts">
import { PhArrowUpRight, PhPlus } from '@phosphor-icons/vue'

import BaseButton from '@/components/ui/BaseButton.vue'
import CopyButton from '@/components/ui/CopyButton.vue'
import QrCode from '@/components/urls/QrCode.vue'
import { formatDate, prettyUrl } from '@/lib/format'
import type { UrlResponse } from '@/types/api'

defineProps<{ url: UrlResponse }>()
defineEmits<{ again: [] }>()
</script>

<template>
  <div class="flex flex-col gap-5 sm:flex-row sm:items-center">
    <div class="min-w-0 flex-1">
      <p class="text-xs font-medium uppercase tracking-wide text-fg-subtle">Your short link</p>
      <div class="mt-1.5 flex flex-wrap items-center gap-3">
        <a
          :href="url.shortUrl"
          target="_blank"
          rel="noopener"
          class="font-mono text-lg font-medium text-accent underline-offset-4 hover:underline"
        >
          {{ prettyUrl(url.shortUrl) }}
        </a>
        <a
          :href="url.shortUrl"
          target="_blank"
          rel="noopener"
          class="text-fg-subtle transition-colors hover:text-fg"
          aria-label="Open short link in a new tab"
        >
          <PhArrowUpRight :size="18" />
        </a>
      </div>

      <p class="mt-2 truncate text-sm text-fg-muted" :title="url.originalUrl">
        {{ prettyUrl(url.originalUrl) }}
      </p>
      <p v-if="url.expireAt" class="mt-1 text-xs text-fg-subtle">
        Expires {{ formatDate(url.expireAt) }}
      </p>

      <div class="mt-4 flex flex-wrap gap-2.5">
        <CopyButton :value="url.shortUrl" label="Copy link" />
        <BaseButton variant="ghost" @click="$emit('again')">
          <template #icon><PhPlus :size="18" /></template>
          Shorten another
        </BaseButton>
      </div>
    </div>

    <QrCode :value="url.shortUrl" :size="148" />
  </div>
</template>
