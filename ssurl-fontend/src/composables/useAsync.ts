import { ref, shallowRef } from 'vue'

import { ApiError, type NormalizedError, normalizeNetworkError } from '@/lib/problem'

export interface UseAsyncOptions {
  /** Run the task immediately on creation. */
  immediate?: boolean
}

/**
 * Generic async task wrapper exposing idle/loading/success/error state.
 * Keeps components free of repetitive try/catch + loading boilerplate.
 */
export function useAsync<T, Args extends unknown[] = []>(
  task: (...args: Args) => Promise<T>,
  options: UseAsyncOptions = {},
) {
  const data = shallowRef<T | null>(null)
  const error = ref<NormalizedError | null>(null)
  const loading = ref(false)

  async function run(...args: Args): Promise<T | null> {
    loading.value = true
    error.value = null
    try {
      const result = await task(...args)
      data.value = result
      return result
    } catch (err) {
      error.value = err instanceof ApiError ? err : normalizeNetworkError(err)
      return null
    } finally {
      loading.value = false
    }
  }

  function reset(): void {
    data.value = null
    error.value = null
    loading.value = false
  }

  if (options.immediate) {
    void run(...([] as unknown as Args))
  }

  return { data, error, loading, run, reset }
}
