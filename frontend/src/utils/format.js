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
