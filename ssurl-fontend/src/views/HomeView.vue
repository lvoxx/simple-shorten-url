<script setup lang="ts">
import { ref } from 'vue'
import { PhChartLineUp, PhLightning, PhQrCode } from '@phosphor-icons/vue'

import ShortenForm from '@/components/shorten/ShortenForm.vue'
import ShortenResult from '@/components/shorten/ShortenResult.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import { useReveal } from '@/composables/useReveal'
import type { UrlResponse } from '@/types/api'

const created = ref<UrlResponse | null>(null)

function onCreated(url: UrlResponse): void {
  created.value = url
}

const features = [
  {
    icon: PhLightning,
    title: 'Instant links',
    body: 'Paste any URL and get a compact, shareable short link served from the edge.',
  },
  {
    icon: PhChartLineUp,
    title: 'Click tracking',
    body: 'Every visit is counted so you can see which links actually get traffic.',
  },
  {
    icon: PhQrCode,
    title: 'QR built in',
    body: 'Each short link comes with a scannable QR code, ready for print or slides.',
  },
]

const { target: featuresTarget, visible: featuresVisible } = useReveal()
</script>

<template>
  <section class="relative">
    <div class="bg-grid pointer-events-none absolute inset-x-0 top-0 h-[480px]" aria-hidden="true" />

    <div class="relative mx-auto max-w-3xl px-4 pb-8 pt-20 text-center sm:px-6 sm:pt-28">
      <h1 class="text-4xl font-semibold tracking-tight text-fg sm:text-6xl">
        Shorten links.
        <span class="text-accent">Track every click.</span>
      </h1>
      <p class="mx-auto mt-5 max-w-xl text-base text-fg-muted sm:text-lg">
        A fast URL shortener with built-in click analytics and QR codes. No account needed to
        start.
      </p>
    </div>

    <div class="relative mx-auto max-w-2xl px-4 sm:px-6">
      <BaseCard class="p-5 shadow-2xl shadow-black/30 sm:p-6">
        <ShortenResult v-if="created" :url="created" @again="created = null" />
        <ShortenForm v-else @created="onCreated" />
      </BaseCard>
      <p class="mt-3 text-center text-xs text-fg-subtle">
        Anonymous links expire after 7 days. Sign in to keep them and manage clicks.
      </p>
    </div>
  </section>

  <section
    ref="featuresTarget"
    class="reveal mx-auto mt-20 max-w-5xl px-4 sm:mt-28 sm:px-6"
    :class="{ 'is-visible': featuresVisible }"
  >
    <div class="grid gap-4 sm:grid-cols-3">
      <BaseCard v-for="feature in features" :key="feature.title" class="p-5">
        <span class="flex h-10 w-10 items-center justify-center rounded-[--radius-input] bg-accent-soft text-accent">
          <component :is="feature.icon" :size="22" />
        </span>
        <h2 class="mt-4 font-semibold text-fg">{{ feature.title }}</h2>
        <p class="mt-1.5 text-sm leading-relaxed text-fg-muted">{{ feature.body }}</p>
      </BaseCard>
    </div>
  </section>
</template>
