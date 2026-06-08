<script setup lang="ts">
import { PhCheck, PhCopy } from '@phosphor-icons/vue'

import { useCopy } from '@/composables/useCopy'

const props = withDefaults(
  defineProps<{ value: string; label?: string; size?: 'sm' | 'md' }>(),
  { label: 'Copy', size: 'md' },
)

const { copy, copied } = useCopy()

const dims = {
  sm: { h: 'h-9 px-3 text-sm gap-1.5', icon: 16 },
  md: { h: 'h-11 px-4 text-[0.95rem] gap-2', icon: 18 },
} as const
</script>

<template>
  <button
    type="button"
    class="inline-flex items-center justify-center rounded-[--radius-input] border font-medium
           whitespace-nowrap transition-[background,border,color,transform] duration-150 active:scale-[0.98]"
    :class="[
      dims[size].h,
      copied
        ? 'border-success/40 bg-success/12 text-success'
        : 'border-border-strong bg-surface-2 text-fg hover:border-accent/60',
    ]"
    :aria-label="copied ? 'Copied to clipboard' : `${label} to clipboard`"
    @click="copy(props.value)"
  >
    <PhCheck v-if="copied" :size="dims[size].icon" weight="bold" />
    <PhCopy v-else :size="dims[size].icon" />
    <span>{{ copied ? 'Copied' : label }}</span>
  </button>
</template>
