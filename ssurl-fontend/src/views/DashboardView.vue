<script setup lang="ts">
import { ref } from 'vue'

import ShortenForm from '@/components/shorten/ShortenForm.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import UrlList from '@/components/urls/UrlList.vue'
import { useToastStore } from '@/stores/toast'
import type { UrlResponse } from '@/types/api'

const toast = useToastStore()
const list = ref<InstanceType<typeof UrlList> | null>(null)

function onCreated(url: UrlResponse): void {
  list.value?.prepend(url)
  toast.success('Link created', url.shortUrl)
}
</script>

<template>
  <div class="mx-auto max-w-3xl px-4 py-10 sm:px-6 sm:py-14">
    <header class="mb-6">
      <h1 class="text-2xl font-semibold tracking-tight text-fg">Your links</h1>
      <p class="mt-1 text-sm text-fg-muted">Create, track, and manage your short URLs.</p>
    </header>

    <BaseCard class="mb-8 p-5 sm:p-6">
      <ShortenForm @created="onCreated" />
    </BaseCard>

    <UrlList ref="list" />
  </div>
</template>
