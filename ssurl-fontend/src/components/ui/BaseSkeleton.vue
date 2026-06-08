<script setup lang="ts">
// Skeletal placeholder matching the final layout's shape (no generic spinners).
withDefaults(defineProps<{ width?: string; height?: string; rounded?: string }>(), {
  width: '100%',
  height: '1rem',
  rounded: 'var(--radius-input)',
})
</script>

<template>
  <span
    class="shimmer block bg-surface-2"
    :style="{ width, height, borderRadius: rounded }"
    aria-hidden="true"
  />
</template>
