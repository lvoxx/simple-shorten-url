<script setup lang="ts">
import { computed } from 'vue'
import { PhSpinnerGap } from '@phosphor-icons/vue'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger'
type Size = 'sm' | 'md' | 'lg'

const props = withDefaults(
  defineProps<{
    variant?: Variant
    size?: Size
    type?: 'button' | 'submit' | 'reset'
    loading?: boolean
    disabled?: boolean
    block?: boolean
  }>(),
  { variant: 'primary', size: 'md', type: 'button', loading: false, disabled: false, block: false },
)

const variants: Record<Variant, string> = {
  primary:
    'bg-accent text-accent-fg hover:bg-accent-hover shadow-[0_1px_0_rgba(255,255,255,0.12)_inset]',
  secondary:
    'bg-surface-2 text-fg border border-border-strong hover:border-accent/60 hover:text-fg',
  ghost: 'bg-transparent text-fg-muted hover:text-fg hover:bg-surface-2',
  danger: 'bg-danger text-accent-fg hover:bg-danger-hover',
}

const sizes: Record<Size, string> = {
  sm: 'h-9 px-3.5 text-sm gap-1.5',
  md: 'h-11 px-5 text-[0.95rem] gap-2',
  lg: 'h-12 px-6 text-base gap-2',
}

const isDisabled = computed(() => props.disabled || props.loading)
</script>

<template>
  <button
    :type="type"
    :disabled="isDisabled"
    :aria-busy="loading"
    :class="[
      'inline-flex items-center justify-center rounded-[--radius-input] font-medium',
      'whitespace-nowrap transition-[background,border,color,transform] duration-150',
      'active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-55 disabled:active:scale-100',
      variants[variant],
      sizes[size],
      block ? 'w-full' : '',
    ]"
  >
    <PhSpinnerGap v-if="loading" :size="18" weight="bold" class="animate-spin" aria-hidden="true" />
    <slot v-else name="icon" />
    <slot />
  </button>
</template>
