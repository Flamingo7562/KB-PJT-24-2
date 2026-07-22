/**
 * 폼 입력 검증 유틸.
 *
 * 서버가 최종 검증하지만(docs/rules/api.md), 폼 UX 를 위해 프론트에서도 즉시 안내한다.
 * 각 함수는 `{ valid: boolean, message: string }` 를 반환한다(통과 시 message '').
 * 회원가입·사업장 등록·비밀번호 변경 등 폼 화면에서 공통으로 사용한다.
 */

const ok = { valid: true, message: '' }
const fail = (message) => ({ valid: false, message })

/** 필수값 */
export function isRequired(value, label = '필수 항목') {
  const v = typeof value === 'string' ? value.trim() : value
  if (v === '' || v === null || v === undefined) return fail(`${label}을(를) 입력해주세요.`)
  return ok
}

/** 이메일 형식 */
export function isEmail(value) {
  if (!value) return fail('이메일을 입력해주세요.')
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return re.test(value) ? ok : fail('올바른 이메일 형식이 아닙니다.')
}

/** 비밀번호 규칙: 8자 이상, 영문+숫자 포함 */
export function passwordRule(value) {
  if (!value) return fail('비밀번호를 입력해주세요.')
  if (value.length < 8) return fail('비밀번호는 8자 이상이어야 합니다.')
  if (!/[a-zA-Z]/.test(value) || !/\d/.test(value)) {
    return fail('영문과 숫자를 모두 포함해야 합니다.')
  }
  return ok
}

/** 비밀번호 확인 일치 */
export function passwordsMatch(password, confirm) {
  if (!confirm) return fail('비밀번호 확인을 입력해주세요.')
  return password === confirm ? ok : fail('비밀번호가 일치하지 않습니다.')
}

/** 아이디: 4~20자 영문/숫자 */
export function loginIdRule(value) {
  if (!value) return fail('아이디를 입력해주세요.')
  return /^[a-zA-Z0-9]{4,20}$/.test(value) ? ok : fail('아이디는 4~20자 영문·숫자입니다.')
}

/** 사업자등록번호: 10자리 숫자(하이픈 허용) */
export function isBusinessNumber(value) {
  if (!value) return fail('사업자등록번호를 입력해주세요.')
  const digits = String(value).replace(/-/g, '')
  return /^\d{10}$/.test(digits) ? ok : fail('사업자등록번호는 숫자 10자리입니다.')
}

/** 전화번호(선택 항목): 값이 있으면 형식 검사 */
export function isPhone(value, { required = false } = {}) {
  if (!value) return required ? fail('전화번호를 입력해주세요.') : ok
  const digits = String(value).replace(/-/g, '')
  return /^0\d{8,10}$/.test(digits) ? ok : fail('올바른 전화번호 형식이 아닙니다.')
}

/** 금액(양의 정수) */
export function isPositiveAmount(value) {
  const n = Number(value)
  if (!Number.isFinite(n) || n <= 0) return fail('금액을 올바르게 입력해주세요.')
  if (!Number.isInteger(n)) return fail('금액은 원 단위 정수로 입력해주세요.')
  return ok
}
