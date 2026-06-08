<script setup lang="ts">
import { ref } from 'vue'
import { z } from 'zod'
import { PhArrowRight, PhWarningCircle } from '@phosphor-icons/vue'

import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import { useForm } from '@/composables/useForm'
import { urlService } from '@/services/urlService'
import type { UrlResponse } from '@/types/api'

const emit = defineEmits<{ created: [url: UrlResponse] }>()

const schema = z.object({
  originalUrl: z
    .string()
    .trim()
    .min(1, 'Paste a URL to shorten')
    .url('Enter a valid URL, including https://'),
  title: z.string().trim().max(100, 'Title must be 100 characters or fewer'),
})

const { values, errors, submitting, formError, submit, reset } = useForm(schema, {
  originalUrl: '',
  title: '',
})

const showTitle = ref(false)

async function onSubmit(): Promise<void> {
  const ok = await submit(async (data) => {
    const created = await urlService.create({
      originalUrl: data.originalUrl,
      title: data.title ? data.title : null,
    })
    emit('created', created)
  })
  if (ok) {
    reset()
    showTitle.value = false
  }
}
</script>

<template>
  <form class="flex flex-col gap-3" novalidate @submit.prevent="onSubmit">
    <div class="flex flex-col gap-3 sm:flex-row sm:items-start">
      <div class="flex-1">
        <BaseInput
          v-model="values.originalUrl"
          label="Long URL"
          type="url"
          placeholder="https://example.com/a-very-long-link"
          autocomplete="off"
          :error="errors.originalUrl"
          required
        />
      </div>
      <BaseButton
        type="submit"
        size="lg"
        :loading="submitting"
        class="sm:mt-[1.85rem]"
      >
        Shorten
        <template #icon><PhArrowRight v-if="!submitting" :size="18" weight="bold" /></template>
      </BaseButton>
    </div>

    <div>
      <button
        v-if="!showTitle"
        type="button"
        class="text-sm font-medium text-fg-subtle transition-colors hover:text-fg-muted"
        @click="showTitle = true"
      >
        + Add a title
      </button>
      <BaseInput
        v-else
        v-model="values.title"
        label="Title"
        placeholder="A label to recognize this link"
        helper="Optional. Up to 100 characters."
        :error="errors.title"
      />
    </div>

    <p
      v-if="formError"
      class="flex items-center gap-2 rounded-[--radius-input] border border-danger/30 bg-danger/10 px-3.5 py-2.5 text-sm text-danger"
      role="alert"
    >
      <PhWarningCircle :size="18" weight="fill" class="shrink-0" />
      {{ formError }}
    </p>
  </form>
</template>
