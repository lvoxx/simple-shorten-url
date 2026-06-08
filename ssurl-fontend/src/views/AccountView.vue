<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { z } from 'zod'

import BaseBadge from '@/components/ui/BaseBadge.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseModal from '@/components/ui/BaseModal.vue'
import { useForm } from '@/composables/useForm'
import { formatDate } from '@/lib/format'
import { ApiError } from '@/lib/problem'
import { userService } from '@/services/userService'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const auth = useAuthStore()
const toast = useToastStore()
const router = useRouter()

const emailSchema = z.object({ email: z.email('Enter a valid email address') })
const { values, errors, submitting, formError, submit } = useForm(emailSchema, {
  email: auth.user?.email ?? '',
})

async function onUpdateEmail(): Promise<void> {
  const ok = await submit(async (data) => {
    const updated = await userService.updateEmail(data.email)
    if (auth.user) auth.user.email = updated.email
  })
  if (ok) toast.success('Email updated')
}

const deleteOpen = ref(false)
const deleteBusy = ref(false)

async function confirmDeleteAccount(): Promise<void> {
  deleteBusy.value = true
  try {
    await userService.deleteAccount()
    auth.clearSession()
    deleteOpen.value = false
    toast.info('Account deleted')
    await router.push('/')
  } catch (err) {
    toast.error('Could not delete account', err instanceof ApiError ? err.message : undefined)
  } finally {
    deleteBusy.value = false
  }
}
</script>

<template>
  <div class="mx-auto max-w-2xl px-4 py-10 sm:px-6 sm:py-14">
    <h1 class="text-2xl font-semibold tracking-tight text-fg">Account</h1>
    <p class="mt-1 text-sm text-fg-muted">Manage your profile and account settings.</p>

    <BaseCard class="mt-8 p-6">
      <h2 class="font-semibold text-fg">Profile</h2>
      <dl class="mt-4 grid gap-3 text-sm sm:grid-cols-2">
        <div>
          <dt class="text-fg-subtle">Username</dt>
          <dd class="mt-0.5 font-mono text-fg">{{ auth.user?.username }}</dd>
        </div>
        <div>
          <dt class="text-fg-subtle">Role</dt>
          <dd class="mt-0.5"><BaseBadge tone="accent">{{ auth.user?.role }}</BaseBadge></dd>
        </div>
        <div>
          <dt class="text-fg-subtle">Member since</dt>
          <dd class="mt-0.5 text-fg">{{ formatDate(auth.user?.createdAt) }}</dd>
        </div>
        <div>
          <dt class="text-fg-subtle">Status</dt>
          <dd class="mt-0.5">
            <BaseBadge :tone="auth.user?.isActive ? 'success' : 'neutral'">
              {{ auth.user?.isActive ? 'Active' : 'Inactive' }}
            </BaseBadge>
          </dd>
        </div>
      </dl>
    </BaseCard>

    <BaseCard class="mt-6 p-6">
      <h2 class="font-semibold text-fg">Email</h2>
      <form class="mt-4 flex flex-col gap-4 sm:flex-row sm:items-start" novalidate @submit.prevent="onUpdateEmail">
        <div class="flex-1">
          <BaseInput
            v-model="values.email"
            label="Email address"
            type="email"
            autocomplete="email"
            :error="errors.email ?? formError ?? undefined"
          />
        </div>
        <BaseButton type="submit" :loading="submitting" class="sm:mt-[1.85rem]">Update</BaseButton>
      </form>
    </BaseCard>

    <BaseCard class="mt-6 border-danger/30 p-6">
      <h2 class="font-semibold text-fg">Delete account</h2>
      <p class="mt-1.5 text-sm text-fg-muted">
        Permanently delete your account. Your links will be detached and stop being managed by you.
      </p>
      <BaseButton variant="danger" class="mt-4" @click="deleteOpen = true">Delete account</BaseButton>
    </BaseCard>

    <BaseModal :open="deleteOpen" title="Delete your account" @close="deleteOpen = false">
      <p>This permanently deletes your account and signs you out. This cannot be undone.</p>
      <template #footer>
        <BaseButton variant="ghost" :disabled="deleteBusy" @click="deleteOpen = false">
          Cancel
        </BaseButton>
        <BaseButton variant="danger" :loading="deleteBusy" @click="confirmDeleteAccount">
          Delete account
        </BaseButton>
      </template>
    </BaseModal>
  </div>
</template>
