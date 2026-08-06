import { reactive, watch } from 'vue'

/**
 * 폼 실시간 검증.
 *
 * 계약: 손대지 않은 필드는 조용하고, 떠난 필드는 검증하며, 떠난 뒤에는 입력마다
 * 재검증해 고치는 즉시 오류가 사라진다. 입력 중인 사용자를 다그치지 않고 고치는 것만
 * 즉시 보상한다.
 *
 * 규칙은 개별 값이 아니라 값 전체를 받는다. 비밀번호 확인처럼 다른 필드에 의존하는
 * 규칙이 있기 때문이다. 값이 바뀌면 touched 된 필드를 모두 다시 검증하므로, 비밀번호를
 * 고치면 확인란의 불일치 오류가 자동으로 사라진다 — 필드 간 의존을 따로 선언하지 않는다.
 *
 * @param {() => object} getValues 현재 값 전체를 돌려주는 getter
 * @param {Record<string, (values: object) => {valid: boolean, message: string}>} rules
 */
export function useFieldValidation(getValues, rules) {
  const fieldNames = Object.keys(rules)
  const errors = reactive(Object.fromEntries(fieldNames.map((name) => [name, ''])))
  const touched = reactive(Object.fromEntries(fieldNames.map((name) => [name, false])))

  function evaluate(name) {
    const result = rules[name](getValues())
    errors[name] = result.valid ? '' : result.message
    return result.valid
  }

  /** 필드를 떠났다. 이후로는 입력마다 재검증한다. */
  function handleBlur(name) {
    touched[name] = true
    evaluate(name)
  }

  /** 제출 직전. 전 필드를 touched 로 올리고 검증한다. */
  function validateAll() {
    let allValid = true
    for (const name of fieldNames) {
      touched[name] = true
      if (!evaluate(name)) allValid = false
    }
    return allValid
  }

  function reset() {
    for (const name of fieldNames) {
      touched[name] = false
      errors[name] = ''
    }
  }

  // 값이 바뀌면 touched 된 필드만 재검증한다. 손대지 않은 필드는 계속 조용하다.
  watch(
    getValues,
    () => {
      for (const name of fieldNames) {
        if (touched[name]) evaluate(name)
      }
    },
    { deep: true }
  )

  return { errors, touched, handleBlur, validateAll, reset }
}
