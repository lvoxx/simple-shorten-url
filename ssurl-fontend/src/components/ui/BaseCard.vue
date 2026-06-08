<script setup lang="ts">
withDefaults(defineProps<{ as?: string; interactive?: boolean }>(), {
  as: 'div',
  interactive: false,
})
</script>

<template>
  <component
    :is="as"
    class="rounded-[--radius-card] border border-border bg-surface"
    :class="
      interactive
        ? 'transition-colors duration-150 hover:border-border-strong'
        : ''
    "
  >
    <slot />
  </component>
</template>
