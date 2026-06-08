import { defineStore } from 'pinia'
import { ref } from 'vue'

export type ToastTone = 'success' | 'error' | 'info'

export interface Toast {
  id: number
  tone: ToastTone
  title: string
  message?: string
}

const DEFAULT_TIMEOUT = 5000

/** Transient, global notifications rendered by AppToast. */
export const useToastStore = defineStore('toast', () => {
  const toasts = ref<Toast[]>([])
  let seq = 0

  function dismiss(id: number): void {
    toasts.value = toasts.value.filter((t) => t.id !== id)
  }

  function push(tone: ToastTone, title: string, message?: string, timeout = DEFAULT_TIMEOUT): number {
    const id = ++seq
    toasts.value = [...toasts.value, { id, tone, title, message }]
    if (timeout > 0) {
      window.setTimeout(() => dismiss(id), timeout)
    }
    return id
  }

  const success = (title: string, message?: string) => push('success', title, message)
  const error = (title: string, message?: string) => push('error', title, message)
  const info = (title: string, message?: string) => push('info', title, message)

  return { toasts, push, dismiss, success, error, info }
})
