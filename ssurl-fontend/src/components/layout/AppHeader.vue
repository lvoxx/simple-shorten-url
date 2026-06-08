<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { PhGauge, PhSignOut, PhUserCircle } from '@phosphor-icons/vue'

import AppLogo from './AppLogo.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const auth = useAuthStore()
const toast = useToastStore()
const router = useRouter()

const username = computed(() => auth.user?.username ?? '')

async function onLogout(): Promise<void> {
  await auth.logout()
  toast.info('Signed out')
  await router.push('/')
}
</script>

<template>
  <header class="sticky top-0 z-40 border-b border-border/80 bg-bg/80 backdrop-blur-md">
    <nav
      class="mx-auto flex h-[68px] max-w-6xl items-center justify-between gap-4 px-4 sm:px-6"
      aria-label="Main"
    >
      <RouterLink to="/" class="rounded" aria-label="ssurl home">
        <AppLogo />
      </RouterLink>

      <div v-if="auth.ready" class="flex items-center gap-2">
        <template v-if="auth.isAuthenticated">
          <RouterLink
            to="/dashboard"
            class="hidden items-center gap-1.5 rounded-[--radius-input] px-3 py-2 text-sm font-medium text-fg-muted transition-colors hover:text-fg sm:inline-flex"
            active-class="text-fg"
          >
            <PhGauge :size="18" />
            Dashboard
          </RouterLink>
          <RouterLink
            to="/account"
            class="inline-flex items-center gap-1.5 rounded-[--radius-input] px-3 py-2 text-sm font-medium text-fg-muted transition-colors hover:text-fg"
            active-class="text-fg"
          >
            <PhUserCircle :size="18" />
            <span class="hidden max-w-[10ch] truncate sm:inline">{{ username }}</span>
          </RouterLink>
          <BaseButton variant="ghost" size="sm" @click="onLogout">
            <template #icon><PhSignOut :size="18" /></template>
            <span class="hidden sm:inline">Sign out</span>
          </BaseButton>
        </template>

        <template v-else>
          <RouterLink
            to="/login"
            class="inline-flex items-center rounded-[--radius-input] px-3 py-2 text-sm font-medium text-fg-muted transition-colors hover:text-fg"
          >
            Log in
          </RouterLink>
          <RouterLink to="/register">
            <BaseButton size="sm">Create account</BaseButton>
          </RouterLink>
        </template>
      </div>
    </nav>
  </header>
</template>
