<script setup lang="ts">
import { RouterLink, useRouter } from 'vue-router'
import { z } from 'zod'
import { PhWarningCircle } from '@phosphor-icons/vue'

import AppLogo from '@/components/layout/AppLogo.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import { useForm } from '@/composables/useForm'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const auth = useAuthStore()
const toast = useToastStore()
const router = useRouter()

// Constraints mirror the backend RegisterRequest validation.
const schema = z.object({
  username: z
    .string()
    .trim()
    .min(3, 'Username must be at least 3 characters')
    .max(50, 'Username must be 50 characters or fewer'),
  email: z.email('Enter a valid email address'),
  password: z
    .string()
    .min(8, 'Password must be at least 8 characters')
    .max(100, 'Password must be 100 characters or fewer'),
})

const { values, errors, submitting, formError, submit } = useForm(schema, {
  username: '',
  email: '',
  password: '',
})

async function onSubmit(): Promise<void> {
  const ok = await submit(async (data) => {
    await auth.register(data)
  })
  if (ok) {
    toast.success('Account created', 'You are now signed in.')
    await router.push('/dashboard')
  }
}
</script>

<template>
  <div class="mx-auto flex max-w-md flex-col items-center px-4 py-16 sm:py-24">
    <RouterLink to="/" class="rounded"><AppLogo :size="30" /></RouterLink>
    <h1 class="mt-6 text-2xl font-semibold tracking-tight text-fg">Create your account</h1>
    <p class="mt-1.5 text-sm text-fg-muted">Keep your links and track clicks over time.</p>

    <BaseCard class="mt-8 w-full p-6">
      <form class="flex flex-col gap-5" novalidate @submit.prevent="onSubmit">
        <BaseInput
          v-model="values.username"
          label="Username"
          autocomplete="username"
          helper="3 to 50 characters."
          :error="errors.username"
          required
        />
        <BaseInput
          v-model="values.email"
          label="Email"
          type="email"
          autocomplete="email"
          :error="errors.email"
          required
        />
        <BaseInput
          v-model="values.password"
          label="Password"
          type="password"
          autocomplete="new-password"
          helper="At least 8 characters."
          :error="errors.password"
          required
        />

        <p
          v-if="formError"
          class="flex items-center gap-2 rounded-[--radius-input] border border-danger/30 bg-danger/10 px-3.5 py-2.5 text-sm text-danger"
          role="alert"
        >
          <PhWarningCircle :size="18" weight="fill" class="shrink-0" />
          {{ formError }}
        </p>

        <BaseButton type="submit" size="lg" block :loading="submitting">Create account</BaseButton>
      </form>
    </BaseCard>

    <p class="mt-6 text-sm text-fg-muted">
      Already have an account?
      <RouterLink to="/login" class="font-medium text-accent hover:underline">Log in</RouterLink>
    </p>
  </div>
</template>
