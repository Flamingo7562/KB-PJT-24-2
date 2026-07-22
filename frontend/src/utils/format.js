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

/** 분(minutes) → "7시간 30분" / "45분" */
export function formatDuration(minutes) {
  const m = Number(minutes) || 0
  const h = Math.floor(m / 60)
  const rest = m % 60
  if (h && rest) return `${h}시간 ${rest}분`
  if (h) return `${h}시간`
  return `${rest}분`
}
