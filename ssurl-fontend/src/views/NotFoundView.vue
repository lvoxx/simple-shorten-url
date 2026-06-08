<script setup lang="ts">
import { RouterLink } from 'vue-router'

import BaseButton from '@/components/ui/BaseButton.vue'
</script>

<template>
  <div class="mx-auto flex max-w-md flex-col items-center px-4 py-28 text-center">
    <p class="font-mono text-5xl font-semibold text-accent">404</p>
    <h1 class="mt-4 text-2xl font-semibold tracking-tight text-fg">Page not found</h1>
    <p class="mt-2 text-sm text-fg-muted">
      The page you are looking for does not exist or has moved.
    </p>
    <RouterLink to="/" class="mt-6">
      <BaseButton>Back to home</BaseButton>
    </RouterLink>
  </div>
</template>
