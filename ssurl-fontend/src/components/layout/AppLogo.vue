<script setup lang="ts">
// Simple generated wordmark mark — geometric link glyph + name.
withDefaults(defineProps<{ size?: number }>(), { size: 26 })
</script>

<template>
  <span class="inline-flex items-center gap-2 font-display font-semibold text-fg">
    <svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="1" y="1" width="22" height="22" rx="6" fill="var(--color-accent)" />
      <path
        d="M9 12a3 3 0 0 1 3-3h1.5a3 3 0 0 1 0 6H12M15 12a3 3 0 0 1-3 3h-1.5a3 3 0 0 1 0-6H12"
        stroke="var(--color-accent-fg)"
        stroke-width="1.6"
        stroke-linecap="round"
      />
    </svg>
    <span class="text-[1.05rem] tracking-tight">ssurl</span>
  </span>
</template>
