<script setup lang="ts">
type Tone = 'neutral' | 'accent' | 'success' | 'danger' | 'warning'

withDefaults(defineProps<{ tone?: Tone }>(), { tone: 'neutral' })

const tones: Record<Tone, string> = {
  neutral: 'bg-surface-2 text-fg-muted border-border',
  accent: 'bg-accent-soft text-accent border-accent/30',
  success: 'bg-success/12 text-success border-success/30',
  danger: 'bg-danger/12 text-danger border-danger/30',
  warning: 'bg-warning/12 text-warning border-warning/30',
}
</script>

<template>
  <span
    class="inline-flex items-center gap-1 rounded-[--radius-pill] border px-2.5 py-0.5 text-xs font-medium"
    :class="tones[tone]"
  >
    <slot />
  </span>
</template>
