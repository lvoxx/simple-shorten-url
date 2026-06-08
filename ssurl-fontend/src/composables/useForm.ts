import { reactive, ref } from 'vue'
import type { ZodType } from 'zod'

import { ApiError } from '@/lib/problem'

/**
 * Lightweight form state backed by a Zod schema.
 *
 * - `values` is reactive and bound with v-model in the template.
 * - `errors` holds one message per field (client validation OR server 400s).
 * - `submit` validates, runs the handler, and maps backend field errors
 *   (the ProblemDetail `errors[]` array) back onto the matching fields.
 */
export function useForm<T extends Record<string, unknown>>(
  schema: ZodType<T>,
  initial: T,
) {
  const values = reactive({ ...initial }) as T
  const errors = reactive<Record<string, string>>({})
  const submitting = ref(false)
  /** Non-field-level error message (e.g. 401 invalid credentials). */
  const formError = ref<string | null>(null)

  function clearErrors(): void {
    for (const key of Object.keys(errors)) delete errors[key]
    formError.value = null
  }

  function setFieldErrors(fieldErrors: Record<string, string>): void {
    for (const [field, message] of Object.entries(fieldErrors)) {
      errors[field] = message
    }
  }

  /** Validate against the schema; populate `errors`. Returns parsed data or null. */
  function validate(): T | null {
    clearErrors()
    const result = schema.safeParse(values)
    if (result.success) return result.data
    for (const issue of result.error.issues) {
      const field = issue.path[0]
      if (typeof field === 'string' && !errors[field]) {
        errors[field] = issue.message
      }
    }
    return null
  }

  async function submit(handler: (data: T) => Promise<void>): Promise<boolean> {
    const parsed = validate()
    if (!parsed) return false

    submitting.value = true
    try {
      await handler(parsed)
      return true
    } catch (err) {
      if (err instanceof ApiError) {
        if (Object.keys(err.fieldErrors).length > 0) {
          setFieldErrors(err.fieldErrors)
        } else {
          formError.value = err.message
        }
      } else {
        formError.value = 'Something went wrong. Please try again.'
      }
      return false
    } finally {
      submitting.value = false
    }
  }

  function reset(): void {
    Object.assign(values, initial)
    clearErrors()
  }

  return { values, errors, submitting, formError, validate, submit, reset, clearErrors }
}
