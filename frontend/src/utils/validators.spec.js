/**
 * 입력 정규화·검증 단위 테스트 — 승인 계약의 경계값을 고정한다.
 * 프런트와 백엔드가 같은 경계를 쓰지 않으면 화면에서 통과한 값이 서버에서 거부된다.
 */
import { describe, expect, it } from 'vitest'

import {
  isEmail,
  isPhone,
  loginIdRule,
  NAME_MAX_LENGTH,
  nameRule,
  normalizeEmail,
  normalizeLoginId,
  normalizeName,
  normalizePhone,
  PASSWORD_MAX_BYTES,
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  passwordRule
} from '@/utils/validators'

const repeat = (char, count) => char.repeat(count)

describe('정규화', () => {
  it('아이디는 앞뒤 공백을 없애고 소문자로 만든다', () => {
    expect(normalizeLoginId('  Tester01  ')).toBe('tester01')
  })

  it('이메일은 앞뒤 공백을 없애고 소문자로 만든다', () => {
    expect(normalizeEmail(' User@Example.COM ')).toBe('user@example.com')
  })

  it('이름은 공백만 정리하고 대소문자는 보존한다', () => {
    expect(normalizeName('  Kim SaJang  ')).toBe('Kim SaJang')
  })

  it('전화번호는 하이픈과 공백을 제거해 숫자만 남긴다', () => {
    expect(normalizePhone('010-1234-5678')).toBe('01012345678')
    expect(normalizePhone('02 123 4567')).toBe('021234567')
  })

  it('null·undefined 는 빈 문자열로 다룬다', () => {
    expect(normalizeLoginId(null)).toBe('')
    expect(normalizePhone(undefined)).toBe('')
  })
})

describe('passwordRule — 문자 수 경계', () => {
  it(`${PASSWORD_MIN_LENGTH}자 미만은 거부한다`, () => {
    expect(passwordRule(repeat('a', PASSWORD_MIN_LENGTH - 1)).valid).toBe(false)
  })

  it(`${PASSWORD_MIN_LENGTH}자는 통과한다`, () => {
    expect(passwordRule(repeat('a', PASSWORD_MIN_LENGTH)).valid).toBe(true)
  })

  it(`${PASSWORD_MAX_LENGTH}자는 통과한다`, () => {
    expect(passwordRule(repeat('a', PASSWORD_MAX_LENGTH)).valid).toBe(true)
  })

  it(`${PASSWORD_MAX_LENGTH + 1}자는 거부한다`, () => {
    expect(passwordRule(repeat('a', PASSWORD_MAX_LENGTH + 1)).valid).toBe(false)
  })
})

describe('passwordRule — UTF-8 byte 경계', () => {
  // 한글 1자는 UTF-8 로 3 byte 다. BCrypt 가 72 byte 까지만 사용하므로 문자 수만으로는 막지 못한다.
  const koreanAtLimit = repeat('가', PASSWORD_MAX_BYTES / 3) // 24자 = 72 byte
  const koreanOverLimit = repeat('가', PASSWORD_MAX_BYTES / 3 + 1) // 25자 = 75 byte

  it(`정확히 ${PASSWORD_MAX_BYTES} byte 는 통과한다`, () => {
    expect(new TextEncoder().encode(koreanAtLimit).length).toBe(PASSWORD_MAX_BYTES)
    expect(passwordRule(koreanAtLimit).valid).toBe(true)
  })

  it(`${PASSWORD_MAX_BYTES} byte 초과는 문자 수가 상한 이내여도 거부한다`, () => {
    expect(koreanOverLimit.length).toBeLessThanOrEqual(PASSWORD_MAX_LENGTH)
    expect(new TextEncoder().encode(koreanOverLimit).length).toBeGreaterThan(PASSWORD_MAX_BYTES)
    expect(passwordRule(koreanOverLimit).valid).toBe(false)
  })
})

describe('passwordRule — 문자 종류', () => {
  it('영문만으로도 통과한다(조합 규칙을 강제하지 않는다)', () => {
    expect(passwordRule('abcdefgh').valid).toBe(true)
  })

  it('숫자만으로도 통과한다', () => {
    expect(passwordRule('12345678').valid).toBe(true)
  })

  it('빈 값은 거부한다', () => {
    expect(passwordRule('').valid).toBe(false)
  })
})

describe('loginIdRule', () => {
  it('대문자를 섞어 입력해도 정규화 후 통과한다', () => {
    expect(loginIdRule('Tester01').valid).toBe(true)
  })

  it('앞뒤 공백이 있어도 통과한다', () => {
    expect(loginIdRule('  tester01  ').valid).toBe(true)
  })

  it('3자는 거부하고 4자는 통과한다', () => {
    expect(loginIdRule('abc').valid).toBe(false)
    expect(loginIdRule('abcd').valid).toBe(true)
  })

  it('20자는 통과하고 21자는 거부한다', () => {
    expect(loginIdRule(repeat('a', 20)).valid).toBe(true)
    expect(loginIdRule(repeat('a', 21)).valid).toBe(false)
  })

  it('영문·숫자 외 문자는 거부한다', () => {
    expect(loginIdRule('tester_01').valid).toBe(false)
  })
})

describe('nameRule', () => {
  it('공백만 입력하면 거부한다', () => {
    expect(nameRule('   ').valid).toBe(false)
  })

  it('앞뒤 공백을 정리한 뒤 판정한다', () => {
    expect(nameRule('  김사장  ').valid).toBe(true)
  })

  it(`${NAME_MAX_LENGTH}자는 통과하고 ${NAME_MAX_LENGTH + 1}자는 거부한다`, () => {
    expect(nameRule(repeat('가', NAME_MAX_LENGTH)).valid).toBe(true)
    expect(nameRule(repeat('가', NAME_MAX_LENGTH + 1)).valid).toBe(false)
  })

  it('공백을 제거하면 상한 이내인 값은 통과한다', () => {
    expect(nameRule(`  ${repeat('가', NAME_MAX_LENGTH)}  `).valid).toBe(true)
  })
})

describe('isEmail', () => {
  it('앞뒤 공백과 대문자를 정규화한 뒤 판정한다', () => {
    expect(isEmail(' User@Example.COM ').valid).toBe(true)
  })

  it('형식이 아니면 거부한다', () => {
    expect(isEmail('user@').valid).toBe(false)
  })
})

describe('isPhone', () => {
  it('하이픈이 있어도 정규화 후 통과한다', () => {
    expect(isPhone('010-1234-5678').valid).toBe(true)
  })

  it('0 으로 시작하는 9~11자리를 허용한다', () => {
    expect(isPhone('021234567').valid).toBe(true) // 9자리
    expect(isPhone('01012345678').valid).toBe(true) // 11자리
  })

  it('8자리와 12자리는 거부한다', () => {
    expect(isPhone('02123456').valid).toBe(false)
    expect(isPhone('010123456789').valid).toBe(false)
  })

  it('0 으로 시작하지 않으면 거부한다', () => {
    expect(isPhone('11012345678').valid).toBe(false)
  })

  it('선택 항목이라 빈 값은 통과하고, required 면 거부한다', () => {
    expect(isPhone('').valid).toBe(true)
    expect(isPhone('', { required: true }).valid).toBe(false)
  })
})
