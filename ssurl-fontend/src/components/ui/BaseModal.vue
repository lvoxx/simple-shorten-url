<script setup lang="ts">
import { ref, useId, watch } from 'vue'
import { onKeyStroke } from '@vueuse/core'
import { PhX } from '@phosphor-icons/vue'

const props = defineProps<{ open: boolean; title: string }>()
const emit = defineEmits<{ close: [] }>()

const titleId = `modal-${useId()}`
const panel = ref<HTMLElement | null>(null)

function close(): void {
  emit('close')
}

onKeyStroke('Escape', () => {
  if (props.open) close()
})

watch(
  () => props.open,
  (open) => {
    document.body.style.overflow = open ? 'hidden' : ''
    if (open) {
      // Move focus into the dialog once it renders.
      requestAnimationFrame(() => panel.value?.focus())
    }
  },
)
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="open"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
        @click.self="close"
      >
        <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" aria-hidden="true" />
        <div
          ref="panel"
          role="dialog"
          aria-modal="true"
          :aria-labelledby="titleId"
          tabindex="-1"
          class="relative w-full max-w-md rounded-[--radius-card] border border-border bg-surface
                 p-6 shadow-2xl outline-none"
        >
          <div class="mb-4 flex items-start justify-between gap-4">
            <h2 :id="titleId" class="text-lg font-semibold text-fg">{{ title }}</h2>
            <button
              type="button"
              class="rounded-[--radius-input] p-1 text-fg-subtle transition-colors hover:text-fg"
              aria-label="Close dialog"
              @click="close"
            >
              <PhX :size="20" />
            </button>
          </div>
          <div class="text-sm text-fg-muted"><slot /></div>
          <div v-if="$slots.footer" class="mt-6 flex justify-end gap-3">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>
