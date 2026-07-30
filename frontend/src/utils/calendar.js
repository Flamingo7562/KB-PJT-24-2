/**
 * 달력(월 그리드) 계산 유틸 — 순수 함수만 둔다(DOM·API 의존 없음).
 *
 * 날짜 표기 규약(docs/rules/api.md): 날짜는 항상 `YYYY-MM-DD` 문자열("날짜 키")로 주고받는다.
 * 서버 응답의 `workDate` 도 같은 형식이므로, 화면에서는 Date 객체로 바꾸지 않고
 * **문자열 그대로 비교·그룹핑**한다. (`YYYY-MM-DD` 는 사전순 비교 = 시간순 비교라 안전하고,
 * `new Date('2026-07-22')` 가 UTC 로 해석되어 하루 밀리는 문제도 피할 수 있다.)
 *
 * "월 키"는 `YYYY-MM` 문자열이다. 캘린더가 보고 있는 달을 이 한 값으로 표현한다.
 */

/** 캘린더 요일 헤더(일요일 시작 — 한국 달력 관례). */
export const WEEKDAY_LABELS = ['일', '월', '화', '수', '목', '금', '토']

/** 숫자를 2자리로 채운다. 7 → "07" padStart는 앞을 0으로 채워 길이를 2로 맞추는 역할 */
function pad2(n) {
  return String(n).padStart(2, '0')
}

/** 날짜 포맷팅. Date → 날짜 키. 로컬 시간 기준이라 시차로 하루가 밀리지 않는다. */
export function toDateKey(date) {
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`
}

/** 오늘 날짜를 포맷팅해서 반환. 오늘 날짜 키. 캘린더의 '오늘' 표시에 쓴다. */
export function todayKey() {
  return toDateKey(new Date())
}

/** 날짜를 월까지 포맷팅. 날짜 키 → 월 키. "2026-07-22" → "2026-07" */
export function toMonthKey(dateKey) {
  return String(dateKey).slice(0, 7)
}

/** 현재 날짜를 월까지 포맷팅. 이번 달의 월 키. 캘린더 초기값. */
export function currentMonthKey() {
  return toMonthKey(todayKey())
}

/** 연-월을 분리. 월 키 → { year, month } (month 는 1~12). */
export function parseMonthKey(monthKey) {
  const [year, month] = String(monthKey).split('-').map(Number)
  return { year, month }
}

/**
 * 월 키를 delta 개월만큼 이동한다. shiftMonth('2026-01', -1) → '2025-12'
 * Date 의 월 오버플로 처리(13월 → 다음 해 1월)를 그대로 이용한다.
 */
export function shiftMonth(monthKey, delta) {
  const { year, month } = parseMonthKey(monthKey)
  const moved = new Date(year, month - 1 + delta, 1)
  return `${moved.getFullYear()}-${pad2(moved.getMonth() + 1)}`
}

/**
 * 해당 월의 조회 구간 → { from, to } 날짜 키.
 * 목록 API 의 기간 파라미터로 그대로 넘긴다(양끝 포함).
 */
export function monthRange(monthKey) {
  const { year, month } = parseMonthKey(monthKey)
  const lastDay = new Date(year, month, 0).getDate() // 다음 달 0일 = 이번 달 말일
  return { from: `${monthKey}-01`, to: `${monthKey}-${pad2(lastDay)}` }
}

/**
 * 월 그리드 셀 목록 → [{ dateKey, day, inMonth, weekday }]
 *
 * 일요일부터 시작하도록 앞쪽을 지난달 날짜로, 뒤쪽을 다음달 날짜로 채워
 * 항상 7의 배수(4~6주)가 되게 만든다. `inMonth` 가 false 인 셀은 흐리게 표시한다.
 */
export function buildMonthGrid(monthKey) {
  const { year, month } = parseMonthKey(monthKey)
  const firstWeekday = new Date(year, month - 1, 1).getDay() // 0(일)~6(토)
  const daysInMonth = new Date(year, month, 0).getDate()

  // 앞 여백(지난달) + 이번달 일수를 7의 배수로 올림한 만큼 셀을 만든다.
  const cellCount = Math.ceil((firstWeekday + daysInMonth) / 7) * 7

  // '_'는 사용하지 않는 매개변수라는 의미
  return Array.from({ length: cellCount }, (_, i) => {
    // 이번 달 1일을 기준으로 앞뒤로 밀어 실제 날짜를 구한다(지난달·다음달 자동 보정).
    const date = new Date(year, month - 1, 1 - firstWeekday + i)
    return {
      dateKey: toDateKey(date),
      day: date.getDate(),
      inMonth: date.getMonth() === month - 1,
      weekday: date.getDay()
    }
  })
}

/** 월 키 → 화면 표기. "2026-07" → "2026년 7월" */
export function formatMonthLabel(monthKey) {
  const { year, month } = parseMonthKey(monthKey)
  return `${year}년 ${month}월`
}

/** 날짜 키 → 요일까지 붙인 표기. "2026-07-22" → "2026.07.22 (수)" */
export function formatDateKeyWithWeekday(dateKey) {
  const [year, month, day] = String(dateKey).split('-').map(Number)
  const weekday = WEEKDAY_LABELS[new Date(year, month - 1, day).getDay()]
  return `${year}.${pad2(month)}.${pad2(day)} (${weekday})`
}

/**
 * 날짜별로 항목을 묶는다 → { 'YYYY-MM-DD': [item, ...] }
 * 목록 뷰와 캘린더 뷰가 **같은 조회 결과 배열**을 공유하도록, 캘린더는 이 그룹핑만 추가로 쓴다.
 * @param {Array} items 조회한 근무 목록
 * @param {(item:any) => string} getDateKey 항목에서 날짜 키를 꺼내는 함수
 */
export function groupByDateKey(items, getDateKey) {
  const grouped = {}
  for (const item of items ?? []) {
    const key = getDateKey(item)
    if (!key) continue
    ;(grouped[key] ??= []).push(item)
  }
  return grouped
}
