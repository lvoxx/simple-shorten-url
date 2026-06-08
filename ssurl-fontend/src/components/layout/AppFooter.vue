<script setup lang="ts">
import AppLogo from './AppLogo.vue'
</script>

<template>
  <footer class="mt-auto border-t border-border/80">
    <div
      class="mx-auto flex max-w-6xl flex-col items-center justify-between gap-3 px-4 py-8 sm:flex-row sm:px-6"
    >
      <AppLogo :size="22" />
      <p class="text-sm text-fg-subtle">Fast, tracked short links.</p>
      <p class="font-mono text-xs text-fg-subtle">ssurl</p>
    </div>
  </footer>
</template>
