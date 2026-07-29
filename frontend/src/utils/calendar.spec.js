/**
 * 달력 유틸 단위 테스트 — 캘린더 뷰가 어긋나기 쉬운 경계(월 넘김·말일·그리드 길이)를 고정한다.
 */
import { describe, expect, it } from 'vitest'

import {
  buildMonthGrid,
  formatDateKeyWithWeekday,
  formatMonthLabel,
  groupByDateKey,
  monthRange,
  shiftMonth,
  toMonthKey
} from '@/utils/calendar'

describe('shiftMonth', () => {
  it('연말·연초를 넘어갈 때 해가 함께 바뀐다', () => {
    expect(shiftMonth('2026-01', -1)).toBe('2025-12')
    expect(shiftMonth('2026-12', 1)).toBe('2027-01')
  })
})

describe('monthRange', () => {
  it('말일이 다른 달도 정확히 끝난다(윤년 2월 포함)', () => {
    expect(monthRange('2026-07')).toEqual({ from: '2026-07-01', to: '2026-07-31' })
    expect(monthRange('2026-02')).toEqual({ from: '2026-02-01', to: '2026-02-28' })
    expect(monthRange('2028-02')).toEqual({ from: '2028-02-01', to: '2028-02-29' })
  })
})

describe('buildMonthGrid', () => {
  it('항상 7의 배수 칸이고, 첫 칸은 일요일이다', () => {
    const cells = buildMonthGrid('2026-07')
    expect(cells.length % 7).toBe(0)
    expect(cells[0].weekday).toBe(0)
  })

  it('앞뒤 여백은 지난달·다음달로 표시된다', () => {
    // 2026-07-01 은 수요일 → 앞에 6/28~6/30 세 칸이 붙는다
    const cells = buildMonthGrid('2026-07')
    expect(cells[0]).toMatchObject({ dateKey: '2026-06-28', inMonth: false })
    expect(cells[3]).toMatchObject({ dateKey: '2026-07-01', day: 1, inMonth: true })
    expect(cells.filter((c) => c.inMonth)).toHaveLength(31)
  })
})

describe('groupByDateKey', () => {
  it('같은 날짜의 항목을 한 배열로 묶는다', () => {
    const items = [
      { id: 1, workDate: '2026-07-22' },
      { id: 2, workDate: '2026-07-22' },
      { id: 3, workDate: '2026-07-23' }
    ]
    const grouped = groupByDateKey(items, (item) => item.workDate)
    expect(grouped['2026-07-22']).toHaveLength(2)
    expect(grouped['2026-07-23']).toHaveLength(1)
    expect(grouped['2026-07-24']).toBeUndefined()
  })
})

describe('표시 문자열', () => {
  it('월·날짜 라벨을 한글 표기로 만든다', () => {
    expect(toMonthKey('2026-07-22')).toBe('2026-07')
    expect(formatMonthLabel('2026-07')).toBe('2026년 7월')
    expect(formatDateKeyWithWeekday('2026-07-22')).toBe('2026.07.22 (수)')
  })
})
