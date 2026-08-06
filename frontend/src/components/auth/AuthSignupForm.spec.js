/**
 * 회원가입 폼 실시간 검증 계약 테스트(#238).
 *
 * 손대지 않은 필드는 조용하고, 필드를 떠나면 형식 오류가 뜨고, 고치면 즉시 사라진다.
 * 아이디·이메일 중복확인은 형식 오류와 다른 슬롯(checkErrors)을 쓴다 — 성공은 필드 아래
 * success 문구로, 실패는 계속 필드 오류로 표시하고 토스트는 쓰지 않는다.
 *
 * 아래쪽 4개(중복확인·가입 실패 관련) 테스트는 #146 이 추가한, 서버 오류를 조용히
 * 삼키지 않고 토스트·필드로 표면화하는 계약을 그대로 이어받는다.
 */
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const push = vi.fn()
vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))

vi.mock('@/services/auth', () => ({
  checkLoginId: vi.fn(),
  checkEmail: vi.fn(),
  signup: vi.fn()
}))

import AuthSignupForm from '@/components/auth/AuthSignupForm.vue'
import { checkEmail, checkLoginId, signup } from '@/services/auth'
import { useUiStore } from '@/stores/ui'
import { PASSWORD_MAX_LENGTH, PASSWORD_MIN_LENGTH } from '@/utils/validators'

// placeholder 로 필드를 특정한다 — label 텍스트는 '비밀번호'/'비밀번호 확인'처럼 서로를
// 포함해 접두어 매칭이 애매하지만, placeholder 는 필드마다 고유하다.
function getInput(wrapper, placeholder) {
  return wrapper.find(`input[placeholder="${placeholder}"]`)
}

// 아이디·이메일 두 필드가 모두 '중복확인' 버튼을 갖는다. 템플릿 순서(아이디가 먼저)대로
// [0]=아이디, [1]=이메일. 확인완료 상태에서는 버튼이 사라지므로 남은 버튼만 잡힌다.
function checkButtons(wrapper) {
  return wrapper.findAll('button').filter((b) => b.text().includes('중복확인'))
}

function factory(role = 'OWNER') {
  return mount(AuthSignupForm, { props: { role } })
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

describe('AuthSignupForm', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    push.mockClear()
    checkLoginId.mockReset().mockResolvedValue({ available: true })
    checkEmail.mockReset().mockResolvedValue({ available: true })
    signup.mockReset().mockResolvedValue({ userId: 1 })
  })

  it('입력 중에는 조용하다', async () => {
    const wrapper = factory()

    await getInput(wrapper, 'example@gighub.com').setValue('abc')

    expect(wrapper.find('.msg.error').exists()).toBe(false)
  })

  it('고치면 오류가 즉시 사라진다', async () => {
    const wrapper = factory()
    const email = getInput(wrapper, 'example@gighub.com')

    await email.setValue('abc')
    await email.trigger('blur')
    expect(wrapper.text()).toContain('올바른 이메일 형식이 아닙니다.')

    await email.setValue('test@example.com')

    expect(wrapper.text()).not.toContain('올바른 이메일 형식이 아닙니다.')
  })

  it('비밀번호를 고치면 확인란 오류도 사라진다', async () => {
    const wrapper = factory()
    const password = getInput(wrapper, `${PASSWORD_MIN_LENGTH}~${PASSWORD_MAX_LENGTH}자`)
    const confirm = getInput(wrapper, '비밀번호를 한 번 더 입력')

    await password.setValue('abcdefgh')
    await confirm.setValue('mismatch1')
    await confirm.trigger('blur')
    expect(wrapper.text()).toContain('비밀번호가 일치하지 않습니다.')

    // 확인란이 아니라 비밀번호 쪽을 고쳤다.
    await password.setValue('mismatch1')

    expect(wrapper.text()).not.toContain('비밀번호가 일치하지 않습니다.')
  })

  // 6개 필드 모두 @blur 배선이 살아 있는지 필드별로 고정한다. 예전에 name 필드에서
  // @blur 를 빼도 전체 테스트가 통과했던 사고가 있었다 — 필드 하나라도 배선이 빠지면
  // 그 필드의 케이스만 정확히 실패해야 한다.
  it.each([
    ['loginId', '4~20자 영문·숫자', 'ab', '아이디는 4~20자 영문·숫자입니다.'],
    [
      'password',
      `${PASSWORD_MIN_LENGTH}~${PASSWORD_MAX_LENGTH}자`,
      'abc',
      `비밀번호는 ${PASSWORD_MIN_LENGTH}~${PASSWORD_MAX_LENGTH}자여야 합니다.`
    ],
    ['passwordConfirm', '비밀번호를 한 번 더 입력', 'abc', '비밀번호가 일치하지 않습니다.'],
    ['name', '이름', '   ', '이름을 입력해주세요.'],
    ['email', 'example@gighub.com', 'abc', '올바른 이메일 형식이 아닙니다.'],
    ['phone', '010-0000-0000 (선택)', '123', '올바른 전화번호 형식이 아닙니다.']
  ])('%s 필드를 떠나면 형식 오류를 보여준다', async (_field, placeholder, value, message) => {
    const wrapper = factory()
    const input = getInput(wrapper, placeholder)

    await input.setValue(value)
    await input.trigger('blur')

    expect(wrapper.text()).toContain(message)
  })

  it('중복확인 성공은 필드 아래에 표시한다', async () => {
    checkLoginId.mockResolvedValue({ available: true })
    const wrapper = factory()

    await getInput(wrapper, '4~20자 영문·숫자').setValue('newuser1')
    await checkButtons(wrapper)[0].trigger('click')
    await flushPromises()

    expect(wrapper.find('.msg.success').text()).toBe('사용 가능한 아이디입니다.')
    // 토스트가 아니라 필드로 옮겼다 — 토스트 큐에는 아무것도 없어야 한다.
    const ui = useUiStore()
    expect(ui.toasts).toHaveLength(0)
  })

  it('중복확인 실패는 필드 오류로 표시한다', async () => {
    checkLoginId.mockResolvedValue({ available: false })
    const wrapper = factory()

    await getInput(wrapper, '4~20자 영문·숫자').setValue('taken')
    await checkButtons(wrapper)[0].trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('이미 사용 중인 아이디입니다.')
  })

  // Important 1: 응답이 오는 동안 값이 바뀌면, 그 응답은 지금 화면에 있는 값에 대한
  // 답이 아니다. 무시하지 않으면 서버가 보지도 못한 값에 대해 '확인완료'가 되살아난다.
  it('중복확인 응답이 오기 전에 값이 바뀌면 그 응답은 무시한다', async () => {
    let resolveCheck
    checkLoginId.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveCheck = resolve
        })
    )
    const wrapper = factory()
    const loginIdInput = getInput(wrapper, '4~20자 영문·숫자')

    await loginIdInput.setValue('checked1')
    await checkButtons(wrapper)[0].trigger('click') // 'checked1' 로 요청 시작, 아직 응답 없음

    await loginIdInput.setValue('changed2') // 응답 전에 값을 바꿈

    resolveCheck({ available: true })
    await flushPromises()

    // 서버는 'checked1' 에 대해서만 답했다 — 'changed2' 에 대해 확인완료로 보이면 안 된다.
    expect(wrapper.find('.msg.success').exists()).toBe(false)
    expect(checkButtons(wrapper).some((b) => b.text().includes('중복확인'))).toBe(true)
  })

  // Minor 3: errors 와 checkErrors 가 동시에 채워질 수 있는 상태(빈 폼을 그대로 제출)에서
  // 어느 쪽이 이기는지 고정한다. 형식조차 안 맞는데 "중복확인을 해주세요" 를 보여주는 건
  // 사용자에게 엉뚱한 다음 행동을 시키는 것이라 형식 오류가 항상 먼저 보여야 한다.
  it('형식 오류가 중복확인 안내보다 먼저 보인다', async () => {
    const wrapper = factory()

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('아이디를 입력해주세요.')
    expect(wrapper.text()).not.toContain('아이디 중복확인을 해주세요.')
  })

  it('아이디 중복확인이 실패하면 서버 메시지를 토스트로 보여준다', async () => {
    checkLoginId.mockRejectedValue({
      response: { data: { message: '잠시 후 다시 시도해주세요.' } }
    })
    const ui = useUiStore()
    const toastSpy = vi.spyOn(ui, 'toast')

    const wrapper = factory()
    await getInput(wrapper, '4~20자 영문·숫자').setValue('owner01')
    await checkButtons(wrapper)[0].trigger('click')
    await flushPromises()

    expect(toastSpy).toHaveBeenCalledWith('잠시 후 다시 시도해주세요.', { type: 'danger' })
  })

  it('이메일 중복확인이 실패하면 서버 메시지를 토스트로 보여준다', async () => {
    checkEmail.mockRejectedValue({ response: { data: { message: '잠시 후 다시 시도해주세요.' } } })
    const ui = useUiStore()
    const toastSpy = vi.spyOn(ui, 'toast')

    const wrapper = factory()
    await getInput(wrapper, 'example@gighub.com').setValue('owner@test.com')
    await checkButtons(wrapper)[1].trigger('click')
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

    const wrapper = factory()
    await fillValidForm(wrapper)
    await checkButtons(wrapper)[0].trigger('click')
    await flushPromises()
    await checkButtons(wrapper)[0].trigger('click')
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

    const wrapper = factory()
    await fillValidForm(wrapper)
    await checkButtons(wrapper)[0].trigger('click')
    await flushPromises()
    await checkButtons(wrapper)[0].trigger('click')
    await flushPromises()
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(toastSpy).toHaveBeenCalledWith('서버 오류가 발생했습니다.', { type: 'danger' })
  })
})
