import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import BaseInput from '@/components/ui/BaseInput.vue'

describe('BaseInput', () => {
  it('emits update:modelValue on input', async () => {
    const wrapper = mount(BaseInput, { props: { modelValue: '', label: 'Email' } })
    await wrapper.get('input').setValue('hi@example.com')
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['hi@example.com'])
  })

  it('renders the label and links it to the input', () => {
    const wrapper = mount(BaseInput, { props: { modelValue: '', label: 'Username' } })
    const label = wrapper.get('label')
    const input = wrapper.get('input')
    expect(label.text()).toContain('Username')
    expect(label.attributes('for')).toBe(input.attributes('id'))
  })

  it('shows the error message and marks the input invalid', () => {
    const wrapper = mount(BaseInput, {
      props: { modelValue: '', label: 'Email', error: 'Required' },
    })
    expect(wrapper.get('[role="alert"]').text()).toBe('Required')
    expect(wrapper.get('input').attributes('aria-invalid')).toBe('true')
  })

  it('prefers helper text when there is no error', () => {
    const wrapper = mount(BaseInput, {
      props: { modelValue: '', label: 'Email', helper: 'We never share it' },
    })
    expect(wrapper.text()).toContain('We never share it')
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
  })
})
