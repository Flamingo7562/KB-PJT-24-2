/**
 * Mock opt-in 불변식 — 프로덕션 빌드에서는 설정과 무관하게 mock 이 켜질 수 없다.
 * 플래그를 import 시점에 계산하므로 매 케이스마다 모듈을 다시 읽는다.
 */
import { afterEach, describe, expect, it, vi } from 'vitest'

async function loadFlag() {
  vi.resetModules()
  const module = await import('@/services/mockFlag')
  return module.USE_MOCK
}

afterEach(() => {
  vi.unstubAllEnvs()
})

describe('USE_MOCK', () => {
  it('VITE_USE_MOCK 이 없으면 꺼진다', async () => {
    vi.stubEnv('DEV', true)
    vi.stubEnv('VITE_USE_MOCK', '')
    expect(await loadFlag()).toBe(false)
  })

  it("개발 모드에서 VITE_USE_MOCK='true' 일 때만 켜진다", async () => {
    vi.stubEnv('DEV', true)
    vi.stubEnv('VITE_USE_MOCK', 'true')
    expect(await loadFlag()).toBe(true)
  })

  it("'true' 가 아닌 값은 켜지 않는다", async () => {
    vi.stubEnv('DEV', true)
    vi.stubEnv('VITE_USE_MOCK', '1')
    expect(await loadFlag()).toBe(false)
  })

  it('프로덕션 빌드에서는 VITE_USE_MOCK=true 여도 꺼진다', async () => {
    vi.stubEnv('DEV', false)
    vi.stubEnv('VITE_USE_MOCK', 'true')
    expect(await loadFlag()).toBe(false)
  })
})
