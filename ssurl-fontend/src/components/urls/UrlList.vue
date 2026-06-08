<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { PhLinkSimple, PhWarningCircle } from '@phosphor-icons/vue'

import UrlCard from './UrlCard.vue'
import UrlEditModal from './UrlEditModal.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseModal from '@/components/ui/BaseModal.vue'
import BaseSkeleton from '@/components/ui/BaseSkeleton.vue'
import { urlService } from '@/services/urlService'
import { ApiError } from '@/lib/problem'
import { useToastStore } from '@/stores/toast'
import type { UrlResponse } from '@/types/api'

const PAGE_SIZE = 20

const toast = useToastStore()

const items = ref<UrlResponse[]>([])
const nextCursor = ref<number | null>(null)
const hasNext = ref(false)
const loading = ref(true)
const loadingMore = ref(false)
const loadError = ref<string | null>(null)

const editing = ref<UrlResponse | null>(null)
const editOpen = ref(false)
const deleting = ref<UrlResponse | null>(null)
const deleteBusy = ref(false)

async function load(): Promise<void> {
  loading.value = true
  loadError.value = null
  try {
    const page = await urlService.listMine({ size: PAGE_SIZE })
    items.value = page.content
    nextCursor.value = page.nextCursor
    hasNext.value = page.hasNext
  } catch (err) {
    loadError.value = err instanceof ApiError ? err.message : 'Could not load your links.'
  } finally {
    loading.value = false
  }
}

async function loadMore(): Promise<void> {
  if (!hasNext.value || loadingMore.value) return
  loadingMore.value = true
  try {
    const page = await urlService.listMine({ cursor: nextCursor.value, size: PAGE_SIZE })
    items.value = [...items.value, ...page.content]
    nextCursor.value = page.nextCursor
    hasNext.value = page.hasNext
  } catch (err) {
    toast.error('Could not load more', err instanceof ApiError ? err.message : undefined)
  } finally {
    loadingMore.value = false
  }
}

function openEdit(url: UrlResponse): void {
  editing.value = url
  editOpen.value = true
}

function onUpdated(updated: UrlResponse): void {
  items.value = items.value.map((u) => (u.id === updated.id ? updated : u))
  toast.success('Link updated')
}

function requestDelete(url: UrlResponse): void {
  deleting.value = url
}

async function confirmDelete(): Promise<void> {
  if (!deleting.value) return
  deleteBusy.value = true
  try {
    await urlService.remove(deleting.value.id)
    items.value = items.value.filter((u) => u.id !== deleting.value?.id)
    toast.success('Link deleted')
    deleting.value = null
  } catch (err) {
    toast.error('Could not delete link', err instanceof ApiError ? err.message : undefined)
  } finally {
    deleteBusy.value = false
  }
}

/** Prepend a freshly created link (called by the dashboard's create form). */
function prepend(url: UrlResponse): void {
  items.value = [url, ...items.value]
}

defineExpose({ prepend })

onMounted(load)
</script>

<template>
  <div>
    <!-- Loading skeletons matching the card shape -->
    <div v-if="loading" class="flex flex-col gap-3" aria-hidden="true">
      <BaseCard v-for="n in 3" :key="n" class="p-5">
        <BaseSkeleton width="40%" height="1.1rem" />
        <BaseSkeleton width="60%" height="0.9rem" class="mt-3" />
        <BaseSkeleton width="80%" height="0.8rem" class="mt-2" />
      </BaseCard>
    </div>

    <!-- Load failure -->
    <BaseCard v-else-if="loadError" class="flex flex-col items-center gap-3 p-10 text-center">
      <PhWarningCircle :size="32" class="text-danger" />
      <p class="text-fg">{{ loadError }}</p>
      <BaseButton variant="secondary" size="sm" @click="load">Try again</BaseButton>
    </BaseCard>

    <!-- Composed empty state -->
    <BaseCard v-else-if="items.length === 0" class="flex flex-col items-center gap-3 p-12 text-center">
      <span class="flex h-12 w-12 items-center justify-center rounded-full bg-accent-soft text-accent">
        <PhLinkSimple :size="24" />
      </span>
      <h3 class="text-lg font-semibold text-fg">No links yet</h3>
      <p class="max-w-sm text-sm text-fg-muted">
        Shorten your first URL with the form above. It will show up here with click counts and
        controls.
      </p>
    </BaseCard>

    <!-- List -->
    <template v-else>
      <div class="flex flex-col gap-3">
        <UrlCard
          v-for="url in items"
          :key="url.id"
          :url="url"
          @edit="openEdit"
          @remove="requestDelete"
        />
      </div>

      <div v-if="hasNext" class="mt-5 flex justify-center">
        <BaseButton variant="secondary" :loading="loadingMore" @click="loadMore">
          Load more
        </BaseButton>
      </div>
    </template>

    <UrlEditModal
      :open="editOpen"
      :url="editing"
      @close="editOpen = false"
      @updated="onUpdated"
    />

    <BaseModal :open="deleting !== null" title="Delete link" @close="deleting = null">
      <p>
        Delete the link for
        <span class="font-mono text-fg">{{ deleting?.shortCode }}</span
        >? This cannot be undone and the short URL will stop working.
      </p>
      <template #footer>
        <BaseButton variant="ghost" :disabled="deleteBusy" @click="deleting = null">
          Cancel
        </BaseButton>
        <BaseButton variant="danger" :loading="deleteBusy" @click="confirmDelete">
          Delete
        </BaseButton>
      </template>
    </BaseModal>
  </div>
</template>
