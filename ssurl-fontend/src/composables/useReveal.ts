import { ref } from 'vue'
import { useIntersectionObserver } from '@vueuse/core'

/**
 * One-shot scroll-reveal. Pair the returned `target` ref with the `.reveal`
 * utility class; `is-visible` is toggled when the element enters the viewport.
 * Motivated motion: communicates "new content arriving" as the user scrolls.
 */
export function useReveal(amount = 0.2) {
  const target = ref<HTMLElement | null>(null)
  const visible = ref(false)

  const { stop } = useIntersectionObserver(
    target,
    (entries) => {
      const entry = entries[0]
      if (entry?.isIntersecting) {
        visible.value = true
        stop()
      }
    },
    { threshold: amount },
  )

  return { target, visible }
}
