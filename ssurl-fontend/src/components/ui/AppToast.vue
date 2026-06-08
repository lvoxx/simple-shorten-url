<script setup lang="ts">
import { PhCheckCircle, PhInfo, PhWarningCircle, PhX } from '@phosphor-icons/vue'

import { useToastStore, type ToastTone } from '@/stores/toast'

const toastStore = useToastStore()

const icons = {
  success: PhCheckCircle,
  error: PhWarningCircle,
  info: PhInfo,
} as const

const accents: Record<ToastTone, string> = {
  success: 'text-success',
  error: 'text-danger',
  info: 'text-accent',
}
</script>

<template>
  <div
    class="pointer-events-none fixed inset-x-0 bottom-0 z-[60] flex flex-col items-center gap-2 p-4 sm:items-end"
    role="region"
    aria-label="Notifications"
  >
    <TransitionGroup name="toast">
      <div
        v-for="toast in toastStore.toasts"
        :key="toast.id"
        class="pointer-events-auto flex w-full max-w-sm items-start gap-3 rounded-[--radius-card]
               border border-border bg-surface-2 p-3.5 shadow-xl"
        role="status"
      >
        <component :is="icons[toast.tone]" :size="20" weight="fill" :class="accents[toast.tone]" />
        <div class="min-w-0 flex-1">
          <p class="text-sm font-medium text-fg">{{ toast.title }}</p>
          <p v-if="toast.message" class="mt-0.5 text-sm text-fg-muted">{{ toast.message }}</p>
        </div>
        <button
          type="button"
          class="shrink-0 rounded p-0.5 text-fg-subtle transition-colors hover:text-fg"
          aria-label="Dismiss notification"
          @click="toastStore.dismiss(toast.id)"
        >
          <PhX :size="16" />
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition:
    opacity 0.25s ease,
    transform 0.25s var(--ease-out-expo);
}
.toast-enter-from {
  opacity: 0;
  transform: translateY(12px);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(16px);
}
</style>
