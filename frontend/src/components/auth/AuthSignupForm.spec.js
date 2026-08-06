/**
 * 회원가입 폼 오류 표면 테스트.
 * 가입이 이 브랜치에서 실 서버로 연결되면서, 중복확인·제출 실패가 조용히 삼켜지면
 * (try/finally 뿐이던 원래 코드) 버튼만 다시 눌리고 사용자는 왜 실패했는지 알 수 없다.
 * 로그인 화면과 같은 수준(서버 message 토스트, 실패 없음)의 안내가 있어야 한다.
 */
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }) }))
vi.mock('@/services/auth', () => ({
  checkLoginId: vi.fn(),
  checkEmail: vi.fn(),
  signup: vi.fn()
}))

import AuthSignupForm from '@/components/auth/AuthSignupForm.vue'
import { checkEmail, checkLoginId, signup } from '@/services/auth'
import { useUiStore } from '@/stores/ui'

function mountForm() {
  return mount(AuthSignupForm, { props: { role: 'OWNER' } })
}

/** [0] 아이디 [1] 비밀번호 [2] 비밀번호 확인 [3] 이름 [4] 이메일 [5] 전화번호 */
async function fillValidForm(wrapper) {
  const inputs = wrapper.findAll('input')
  await inputs[0].setValue('owner01')
  await inputs[1].setValue('secret123')
  await inputs[2].setValue('secret123')
  await inputs[3].setValue('김사장')
  await inputs[4].setValue('owner@test.com')
}

// 중복확인 통과 후에는 버튼이 '확인완료' 문구로 바뀌어 사라지므로, 전역 버튼 목록에서
// 텍스트로 찾으면 다른 필드 확인 후 인덱스가 밀린다. 필드 위치로 범위를 좁혀서 찾는다.
// [0] 아이디 [4] 이메일 (.field 순서는 fillValidForm 주석과 동일)
function checkLoginIdButton(wrapper) {
  return wrapper.findAll('.field')[0].find('button')
}

function checkEmailButton(wrapper) {
  return wrapper.findAll('.field')[4].find('button')
}

describe('AuthSignupForm', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    checkLoginId.mockReset().mockResolvedValue({ available: true })
    checkEmail.mockReset().mockResolvedValue({ available: true })
    signup.mockReset().mockResolvedValue({ userId: 1 })
  })

  it('아이디 중복확인이 실패하면 서버 메시지를 토스트로 보여준다', async () => {
    checkLoginId.mockRejectedValue({
      response: { data: { message: '잠시 후 다시 시도해주세요.' } }
    })
    const ui = useUiStore()
    const toastSpy = vi.spyOn(ui, 'toast')

    const wrapper = mountForm()
    await wrapper.findAll('input')[0].setValue('owner01')
    await checkLoginIdButton(wrapper).trigger('click')
    await flushPromises()

    expect(toastSpy).toHaveBeenCalledWith('잠시 후 다시 시도해주세요.', { type: 'danger' })
  })

  it('이메일 중복확인이 실패하면 서버 메시지를 토스트로 보여준다', async () => {
    checkEmail.mockRejectedValue({ response: { data: { message: '잠시 후 다시 시도해주세요.' } } })
    const ui = useUiStore()
    const toastSpy = vi.spyOn(ui, 'toast')

    const wrapper = mountForm()
    await wrapper.findAll('input')[4].setValue('owner@test.com')
    await checkEmailButton(wrapper).trigger('click')
    await flushPromises()

    expect(toastSpy).toHaveBeenCalledWith('잠시 후 다시 시도해주세요.', { type: 'danger' })
  })

  it('가입 요청이 fieldErrors 를 주면 토스트 대신 해당 입력 필드에 오류를 붙인다', async () => {
    // 중복확인 통과 후 제출 사이에 다른 사용자가 같은 아이디로 먼저 가입하면(경합) 서버는
    // 409 와 함께 loginId 필드 오류를 준다.
    const conflict = {
      response: {
        data: { fieldErrors: [{ field: 'loginId', reason: '이미 사용 중인 아이디입니다.' }] }
      },
      fieldErrors: [{ field: 'loginId', reason: '이미 사용 중인 아이디입니다.' }]
    }
    signup.mockRejectedValue(conflict)
    const ui = useUiStore()
    const toastSpy = vi.spyOn(ui, 'toast')

    const wrapper = mountForm()
    await fillValidForm(wrapper)
    await checkLoginIdButton(wrapper).trigger('click')
    await flushPromises()
    await checkEmailButton(wrapper).trigger('click')
    await flushPromises()
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('이미 사용 중인 아이디입니다.')
    expect(toastSpy).not.toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ type: 'danger' })
    )
  })

  it('가입 요청 실패에 매칭되는 필드가 없으면 토스트로 대체한다', async () => {
    signup.mockRejectedValue({ response: { data: { message: '서버 오류가 발생했습니다.' } } })
    const ui = useUiStore()
    const toastSpy = vi.spyOn(ui, 'toast')

    const wrapper = mountForm()
    await fillValidForm(wrapper)
    await checkLoginIdButton(wrapper).trigger('click')
    await flushPromises()
    await checkEmailButton(wrapper).trigger('click')
    await flushPromises()
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(toastSpy).toHaveBeenCalledWith('서버 오류가 발생했습니다.', { type: 'danger' })
  })
})
