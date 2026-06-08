<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import QRCode from 'qrcode'

const props = withDefaults(defineProps<{ value: string; size?: number }>(), { size: 160 })

const dataUrl = ref<string>('')
const failed = ref(false)

watchEffect(async () => {
  if (!props.value) return
  try {
    dataUrl.value = await QRCode.toDataURL(props.value, {
      width: props.size * 2,
      margin: 1,
      color: { dark: '#e9eaee', light: '#00000000' },
      errorCorrectionLevel: 'M',
    })
    failed.value = false
  } catch {
    failed.value = true
  }
})
</script>

<template>
  <div
    class="flex items-center justify-center rounded-[--radius-input] border border-border bg-surface-2 p-2"
    :style="{ width: `${size}px`, height: `${size}px` }"
  >
    <img
      v-if="dataUrl && !failed"
      :src="dataUrl"
      :width="size - 16"
      :height="size - 16"
      alt="QR code for the short link"
    />
    <span v-else-if="failed" class="px-2 text-center text-xs text-fg-subtle">
      QR unavailable
    </span>
  </div>
</template>
