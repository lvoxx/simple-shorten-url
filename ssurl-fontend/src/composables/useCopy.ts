import { useClipboard } from '@vueuse/core'

/**
 * Copy-to-clipboard with transient "copied" feedback.
 * Thin wrapper over @vueuse/core so components don't repeat the timing config.
 */
export function useCopy(copiedDuration = 1800) {
  const { copy, copied, isSupported } = useClipboard({ copiedDuring: copiedDuration, legacy: true })
  return { copy, copied, isSupported }
}
