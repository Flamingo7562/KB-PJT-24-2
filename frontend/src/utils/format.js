/**
 * 금액·날짜 표시 포맷 유틸.
 * 금액은 KRW 정수 기준(소수점 없음). 화면 표기는 여기 함수만 사용한다.
 */

/** 1250000 → "1,250,000원" */
export function formatKRW(amount) {
  const n = Number(amount) || 0
  return `${n.toLocaleString('ko-KR')}원`
}

/** 거래 방향에 따라 부호를 붙인 금액. CREDIT(+) / DEBIT(-) */
export function formatSignedKRW(amount, direction) {
  const n = Math.abs(Number(amount) || 0)
  const sign = direction === 'DEBIT' ? '-' : '+'
  return `${sign}${formatKRW(n)}`
}

/** ISO-8601 문자열 → "MM.DD HH:mm" */
export function formatDateTime(iso) {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${mm}.${dd} ${hh}:${mi}`
}

/** "2026-07-22" 또는 ISO → "2026.07.22" */
export function formatDate(value) {
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}.${mm}.${dd}`
}

/**
 * 시간 표기 → "HH:mm".
 * "09:00", "09:00:00", ISO 문자열 모두 허용.
 */
export function formatTime(value) {
  if (typeof value === 'string' && /^\d{2}:\d{2}/.test(value)) return value.slice(0, 5)
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${hh}:${mi}`
}

/** 시작·종료 시간 → "09:00 ~ 18:00" */
export function formatTimeRange(start, end) {
  const s = formatTime(start)
  const e = formatTime(end)
  if (!s && !e) return ''
  return `${s} ~ ${e}`
}

/** 입력 중인 사업자등록번호에 하이픈을 자동으로 채운다. "1234567890" → "123-45-67890" */
export function formatBusinessNumberInput(value) {
  const digits = String(value).replace(/\D/g, '').slice(0, 10)
  const p1 = digits.slice(0, 3)
  const p2 = digits.slice(3, 5)
  const p3 = digits.slice(5, 10)
  return [p1, p2, p3].filter(Boolean).join('-')
}

/**
 * 입력 중인 전화번호에 하이픈을 자동으로 채운다.
 * 서울(02)은 2자리, 그 외(지역번호·휴대폰)는 3자리 국번 기준으로 구간을 나눈다.
 */
export function formatPhoneInput(value) {
  const digits = String(value).replace(/\D/g, '').slice(0, 11)

  if (digits.startsWith('02')) {
    if (digits.length < 3) return digits
    if (digits.length <= 6) return `${digits.slice(0, 2)}-${digits.slice(2)}`
    if (digits.length <= 9) return `${digits.slice(0, 2)}-${digits.slice(2, 5)}-${digits.slice(5)}`
    return `${digits.slice(0, 2)}-${digits.slice(2, 6)}-${digits.slice(6, 10)}`
  }

  if (digits.length < 4) return digits
  if (digits.length <= 7) return `${digits.slice(0, 3)}-${digits.slice(3)}`
  if (digits.length <= 10) return `${digits.slice(0, 3)}-${digits.slice(3, 6)}-${digits.slice(6)}`
  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7, 11)}`
}

// 편집·이동에 쓰는 제어 키 — 숫자 전용 입력에서도 항상 허용한다.
const DIGIT_INPUT_CONTROL_KEYS = [
  'Backspace',
  'Delete',
  'ArrowLeft',
  'ArrowRight',
  'ArrowUp',
  'ArrowDown',
  'Tab',
  'Home',
  'End'
]

/**
 * 사업자등록번호·전화번호처럼 숫자만 입력받는 필드에서 숫자 외 키 입력을 막는다.
 * @keydown 에 그대로 연결한다: <AppField @keydown="blockNonDigitKeydown" ... />
 */
export function blockNonDigitKeydown(e) {
  if (e.ctrlKey || e.metaKey || e.altKey) return
  if (DIGIT_INPUT_CONTROL_KEYS.includes(e.key)) return
  if (!/^\d$/.test(e.key)) e.preventDefault()
}

/** 분(minutes) → "7시간 30분" / "45분" */
export function formatDuration(minutes) {
  const m = Number(minutes) || 0
  const h = Math.floor(m / 60)
  const rest = m % 60
  if (h && rest) return `${h}시간 ${rest}분`
  if (h) return `${h}시간`
  return `${rest}분`
}
