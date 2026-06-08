<script setup lang="ts">
import { reactive, ref, watch } from 'vue'

import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseModal from '@/components/ui/BaseModal.vue'
import { ApiError } from '@/lib/problem'
import { urlService } from '@/services/urlService'
import type { UrlResponse } from '@/types/api'

const props = defineProps<{ open: boolean; url: UrlResponse | null }>()
const emit = defineEmits<{ close: []; updated: [url: UrlResponse] }>()

interface EditState {
  title: string
  isActive: boolean
  expireAt: string
}

const form = reactive<EditState>({ title: '', isActive: true, expireAt: '' })
const saving = ref(false)
const error = ref<string | null>(null)

/** Backend LocalDateTime ("2026-06-08T10:00:00") → <input datetime-local> value. */
function toLocalInput(iso: string | null): string {
  if (!iso) return ''
  return iso.slice(0, 16)
}

watch(
  () => props.url,
  (url) => {
    if (!url) return
    form.title = url.title ?? ''
    form.isActive = url.isActive
    form.expireAt = toLocalInput(url.expireAt)
    error.value = null
  },
  { immediate: true },
)

async function onSave(): Promise<void> {
  if (!props.url) return
  saving.value = true
  error.value = null
  try {
    const updated = await urlService.update(props.url.id, {
      title: form.title ? form.title : null,
      isActive: form.isActive,
      expireAt: form.expireAt ? form.expireAt : null,
    })
    emit('updated', updated)
    emit('close')
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : 'Could not save changes.'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <BaseModal :open="open" title="Edit link" @close="emit('close')">
    <form class="flex flex-col gap-4" novalidate @submit.prevent="onSave">
      <BaseInput
        v-model="form.title"
        label="Title"
        placeholder="A label for this link"
        :error="error ?? undefined"
      />

      <BaseInput v-model="form.expireAt" label="Expires at" type="datetime-local" helper="Leave empty for no expiry." />

      <label class="flex items-center gap-3 text-sm text-fg">
        <input
          v-model="form.isActive"
          type="checkbox"
          class="h-4 w-4 rounded border-border-strong accent-[var(--color-accent)]"
        />
        Link is active
      </label>
    </form>

    <template #footer>
      <BaseButton variant="ghost" :disabled="saving" @click="emit('close')">Cancel</BaseButton>
      <BaseButton :loading="saving" @click="onSave">Save changes</BaseButton>
    </template>
  </BaseModal>
</template>
