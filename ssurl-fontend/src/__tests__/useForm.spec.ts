import { describe, expect, it } from 'vitest'
import { z } from 'zod'

import { useForm } from '@/composables/useForm'
import { ApiError, normalizeProblem } from '@/lib/problem'

const schema = z.object({
  username: z.string().min(3, 'too short'),
  email: z.email('bad email'),
})

function makeForm() {
  return useForm(schema, { username: '', email: '' })
}

describe('useForm', () => {
  it('populates field errors on invalid input and blocks submission', async () => {
    const form = makeForm()
    form.values.username = 'ab'
    form.values.email = 'nope'

    let handlerCalled = false
    const ok = await form.submit(async () => {
      handlerCalled = true
    })

    expect(ok).toBe(false)
    expect(handlerCalled).toBe(false)
    expect(form.errors.username).toBe('too short')
    expect(form.errors.email).toBe('bad email')
  })

  it('maps backend ProblemDetail field errors onto fields', async () => {
    const form = makeForm()
    form.values.username = 'valid'
    form.values.email = 'user@example.com'

    const ok = await form.submit(async () => {
      throw new ApiError(normalizeProblem(400, { errors: ['username: already taken'] }))
    })

    expect(ok).toBe(false)
    expect(form.errors.username).toBe('already taken')
    expect(form.formError.value).toBeNull()
  })

  it('surfaces non-field errors as a form-level message', async () => {
    const form = makeForm()
    form.values.username = 'valid'
    form.values.email = 'user@example.com'

    await form.submit(async () => {
      throw new ApiError(normalizeProblem(401, { detail: 'Invalid credentials' }))
    })

    expect(form.formError.value).toBe('Invalid credentials')
  })
})
