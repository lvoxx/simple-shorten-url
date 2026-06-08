<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
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
const route = useRoute()

const redirectTo = computed(() => (typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'))

const schema = z.object({
  username: z.string().trim().min(1, 'Enter your username'),
  password: z.string().min(1, 'Enter your password'),
})

const { values, errors, submitting, formError, submit } = useForm(schema, {
  username: '',
  password: '',
})

async function onSubmit(): Promise<void> {
  const ok = await submit(async (data) => {
    await auth.login(data)
  })
  if (ok) {
    toast.success(`Welcome back, ${auth.user?.username}`)
    await router.push(redirectTo.value)
  }
}
</script>

<template>
  <div class="mx-auto flex max-w-md flex-col items-center px-4 py-16 sm:py-24">
    <RouterLink to="/" class="rounded"><AppLogo :size="30" /></RouterLink>
    <h1 class="mt-6 text-2xl font-semibold tracking-tight text-fg">Log in to your account</h1>
    <p class="mt-1.5 text-sm text-fg-muted">Manage your links and track clicks.</p>

    <BaseCard class="mt-8 w-full p-6">
      <form class="flex flex-col gap-5" novalidate @submit.prevent="onSubmit">
        <BaseInput
          v-model="values.username"
          label="Username"
          autocomplete="username"
          :error="errors.username"
          required
        />
        <BaseInput
          v-model="values.password"
          label="Password"
          type="password"
          autocomplete="current-password"
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

        <BaseButton type="submit" size="lg" block :loading="submitting">Log in</BaseButton>
      </form>
    </BaseCard>

    <p class="mt-6 text-sm text-fg-muted">
      No account?
      <RouterLink to="/register" class="font-medium text-accent hover:underline">
        Create one
      </RouterLink>
    </p>
  </div>
</template>
