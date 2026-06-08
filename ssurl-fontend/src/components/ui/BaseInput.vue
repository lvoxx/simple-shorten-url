<script setup lang="ts">
import { computed, useId } from 'vue'

const props = withDefaults(
  defineProps<{
    modelValue: string
    label: string
    type?: string
    placeholder?: string
    helper?: string
    error?: string
    autocomplete?: string
    required?: boolean
    disabled?: boolean
  }>(),
  { type: 'text', required: false, disabled: false },
)

const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const uid = useId()
const inputId = `in-${uid}`
const helperId = `help-${uid}`
const errorId = `err-${uid}`

const describedBy = computed(() => {
  const ids: string[] = []
  if (props.error) ids.push(errorId)
  else if (props.helper) ids.push(helperId)
  return ids.length ? ids.join(' ') : undefined
})

function onInput(event: Event): void {
  emit('update:modelValue', (event.target as HTMLInputElement).value)
}
</script>

<template>
  <div class="flex flex-col gap-2">
    <label :for="inputId" class="text-sm font-medium text-fg">
      {{ label }}
      <span v-if="required" class="text-danger" aria-hidden="true">*</span>
    </label>

    <input
      :id="inputId"
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :autocomplete="autocomplete"
      :required="required"
      :disabled="disabled"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="describedBy"
      class="h-11 w-full rounded-[--radius-input] border bg-surface px-3.5 text-[0.95rem] text-fg
             placeholder:text-fg-subtle transition-colors duration-150
             hover:border-border-strong focus:border-accent
             disabled:cursor-not-allowed disabled:opacity-60"
      :class="error ? 'border-danger' : 'border-border'"
      @input="onInput"
    />

    <p v-if="error" :id="errorId" class="text-sm text-danger" role="alert">{{ error }}</p>
    <p v-else-if="helper" :id="helperId" class="text-sm text-fg-subtle">{{ helper }}</p>
  </div>
</template>
