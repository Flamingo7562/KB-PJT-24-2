/**
 * 실시간 검증 계약 테스트.
 * 손대지 않은 필드는 조용하고, 떠난 필드는 검증되며, 고치면 즉시 오류가 사라진다.
 * 규칙이 값 전체를 받는 이유는 비밀번호 확인처럼 다른 필드에 의존하는 규칙 때문이다.
 */
import { reactive, nextTick } from 'vue'
import { describe, expect, it } from 'vitest'

import { useFieldValidation } from '@/composables/useFieldValidation'

function setup() {
  const form = reactive({ password: '', passwordConfirm: '' })
  const validation = useFieldValidation(() => form, {
    password: (v) => ({
      valid: v.password.length >= 8,
      message: v.password.length >= 8 ? '' : '8자 이상 입력하세요'
    }),
    passwordConfirm: (v) => ({
      valid: v.passwordConfirm === v.password,
      message: v.passwordConfirm === v.password ? '' : '비밀번호가 일치하지 않습니다'
    })
  })
  return { form, ...validation }
}

describe('useFieldValidation', () => {
  it('손대지 않은 필드는 검증하지 않는다', async () => {
    const { form, errors } = setup()

    form.password = 'abc'
    await nextTick()

    expect(errors.password).toBe('')
  })

  it('필드를 떠나면 검증한다', () => {
    const { form, errors, handleBlur } = setup()

    form.password = 'abc'
    handleBlur('password')

    expect(errors.password).toBe('8자 이상 입력하세요')
  })

  it('떠난 뒤에는 입력할 때마다 재검증해 고치면 즉시 사라진다', async () => {
    const { form, errors, handleBlur } = setup()
    form.password = 'abc'
    handleBlur('password')

    form.password = 'abcdefgh'
    await nextTick()

    expect(errors.password).toBe('')
  })

  it('다른 필드를 고치면 touched 된 필드도 다시 검증한다', async () => {
    const { form, errors, handleBlur } = setup()
    form.password = 'abcdefgh'
    form.passwordConfirm = 'abcdefg'
    handleBlur('passwordConfirm')
    expect(errors.passwordConfirm).toBe('비밀번호가 일치하지 않습니다')

    // 확인란이 아니라 비밀번호 쪽을 고쳤다.
    form.password = 'abcdefg'
    await nextTick()

    expect(errors.passwordConfirm).toBe('')
  })

  it('손대지 않은 필드는 다른 필드가 바뀌어도 조용하다', async () => {
    const { form, errors, handleBlur } = setup()
    form.password = 'abc'
    handleBlur('password')

    form.passwordConfirm = 'zzz'
    await nextTick()

    expect(errors.passwordConfirm).toBe('')
  })

  it('validateAll 은 전 필드를 검증하고 결과를 알려준다', () => {
    const { form, errors, validateAll } = setup()
    form.password = 'abc'
    // passwordConfirm 을 비워 두면 이 필드 자체의 규칙(password 와 일치)이 깨져
    // '패스워드만 틀렸다'는 시나리오를 만들 수 없다. password 와 맞춰 두어야
    // password 필드만 유효성 실패로 남는다.
    form.passwordConfirm = 'abc'

    const ok = validateAll()

    expect(ok).toBe(false)
    expect(errors.password).toBe('8자 이상 입력하세요')
    expect(errors.passwordConfirm).toBe('')
  })

  it('전부 유효하면 validateAll 이 true 를 준다', () => {
    const { form, validateAll } = setup()
    form.password = 'abcdefgh'
    form.passwordConfirm = 'abcdefgh'

    expect(validateAll()).toBe(true)
  })

  it('validateAll 은 앞쪽 필드가 유효해도 뒤쪽 필드가 무효면 false 를 준다', () => {
    const { form, validateAll } = setup()
    form.password = 'abcdefgh'
    form.passwordConfirm = 'zzz'

    expect(validateAll()).toBe(false)
  })

  it('validateAll 은 손대지 않았던 필드도 touched 로 표시해 이후 입력마다 재검증한다', async () => {
    const { form, errors, validateAll } = setup()
    form.password = 'abc'

    // 아무 필드도 blur 하지 않은 채 제출을 시도한다.
    validateAll()
    expect(errors.password).toBe('8자 이상 입력하세요')

    // 제출 실패 후에는 건드리지 않았던 필드도 입력마다 실시간으로 재검증되어야 한다.
    form.password = 'abcdefgh'
    await nextTick()

    expect(errors.password).toBe('')
  })

  it('reset 은 오류와 touched 를 모두 비운다', async () => {
    const { form, errors, handleBlur, reset } = setup()
    form.password = 'abc'
    handleBlur('password')

    reset()
    expect(errors.password).toBe('')

    form.password = 'xy'
    await nextTick()
    expect(errors.password).toBe('')
  })
})
