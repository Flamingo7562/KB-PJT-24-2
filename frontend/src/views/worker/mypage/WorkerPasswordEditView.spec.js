/**
 * 알바생 비밀번호 변경 화면 — 오류 귀속 계약 테스트.
 * PATCH /api/users/me/password 는 아직 서버에 없다(#187). 이전에는 원인과 무관하게
 * 모든 실패를 '현재 비밀번호가 일치하지 않아요' 로 단정했다 — 서버가 실제로 그 필드를
 * 지목했을 때만 그 문구를 보여줘야 한다.
 */
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const back = vi.fn()
vi.mock('vue-router', () => ({ useRouter: () => ({ back }) }))
vi.mock('@/services/users', () => ({ changePassword: vi.fn() }))

import { changePassword } from '@/services/users'
import { useUiStore } from '@/stores/ui'
import WorkerPasswordEditView from '@/views/worker/mypage/WorkerPasswordEditView.vue'

/** [0] 현재 비밀번호 [1] 새 비밀번호 [2] 새 비밀번호 확인 */
async function fillValidForm(wrapper) {
  const inputs = wrapper.findAll('input')
  await inputs[0].setValue('current-pw1')
  await inputs[1].setValue('newpassword1')
  await inputs[2].setValue('newpassword1')
}

describe('WorkerPasswordEditView 오류 귀속', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    back.mockClear()
    changePassword.mockReset()
  })

  it('필드를 지목하지 않은 실패는 현재 비밀번호 오류로 표시하지 않는다', async () => {
    changePassword.mockRejectedValue({ response: { status: 500, data: {} } })
    const toastSpy = vi.spyOn(useUiStore(), 'toast')

    const wrapper = mount(WorkerPasswordEditView)
    await fillValidForm(wrapper)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.findAll('.field')[0].find('.msg.error').exists()).toBe(false)
    expect(toastSpy).toHaveBeenCalledWith('비밀번호 변경에 실패했어요.', { type: 'danger' })
  })

  it('서버가 현재 비밀번호 필드를 지목하면 그 필드 아래에 표시한다', async () => {
    const fieldError = { field: 'currentPassword', reason: '현재 비밀번호가 올바르지 않습니다.' }
    changePassword.mockRejectedValue({
      response: { status: 400, data: { fieldErrors: [fieldError] } },
      fieldErrors: [fieldError]
    })

    const wrapper = mount(WorkerPasswordEditView)
    await fillValidForm(wrapper)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.findAll('.field')[0].find('.msg.error').text()).toBe(
      '현재 비밀번호가 올바르지 않습니다.'
    )
  })

  it('서버가 새 비밀번호 필드를 지목하면 그 필드 아래에 표시하고 토스트는 띄우지 않는다', async () => {
    const fieldError = { field: 'newPassword', reason: '이전에 사용한 비밀번호는 쓸 수 없습니다.' }
    changePassword.mockRejectedValue({
      response: { status: 400, data: { fieldErrors: [fieldError] } },
      fieldErrors: [fieldError]
    })
    const toastSpy = vi.spyOn(useUiStore(), 'toast')

    const wrapper = mount(WorkerPasswordEditView)
    await fillValidForm(wrapper)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.findAll('.field')[1].find('.msg.error').text()).toBe(
      '이전에 사용한 비밀번호는 쓸 수 없습니다.'
    )
    expect(toastSpy).not.toHaveBeenCalled()
  })

  it('fieldErrors 없이 message 만 있으면 필드가 아니라 폼 레벨 토스트로 보여준다', async () => {
    changePassword.mockRejectedValue({
      response: { status: 500, data: { message: '잠시 후 다시 시도해주세요.' } }
    })
    const toastSpy = vi.spyOn(useUiStore(), 'toast')

    const wrapper = mount(WorkerPasswordEditView)
    await fillValidForm(wrapper)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.findAll('.field')[0].find('.msg.error').exists()).toBe(false)
    expect(toastSpy).toHaveBeenCalledWith('잠시 후 다시 시도해주세요.', { type: 'danger' })
  })

  it('성공하면 토스트를 보여주고 이전 화면으로 돌아간다', async () => {
    changePassword.mockResolvedValue()
    const toastSpy = vi.spyOn(useUiStore(), 'toast')

    const wrapper = mount(WorkerPasswordEditView)
    await fillValidForm(wrapper)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(toastSpy).toHaveBeenCalledWith('비밀번호가 변경됐어요.', { type: 'success' })
    expect(back).toHaveBeenCalledTimes(1)
  })
})

describe('WorkerPasswordEditView 준비 중 안내', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('폼 상단에 준비 중 안내를 보여주고 제출 버튼을 비활성화한다', () => {
    const wrapper = mount(WorkerPasswordEditView)

    expect(wrapper.text()).toContain('비밀번호 변경은 준비 중입니다')
    expect(wrapper.find('button[type="submit"]').attributes('disabled')).toBeDefined()
  })
})
