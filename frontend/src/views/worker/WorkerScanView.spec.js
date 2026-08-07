import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/services/worker', () => ({ scan: vi.fn() }))

import { scan } from '@/services/worker'
import WorkerScanView from '@/views/worker/WorkerScanView.vue'

const POSITION = {
  coords: { latitude: 37.123456789, longitude: 127.123456789, accuracy: 12.345 },
  timestamp: Date.parse('2026-08-07T01:00:00Z')
}

function mountView() {
  return mount(WorkerScanView, {
    global: { plugins: [createPinia()], stubs: { teleport: true } }
  })
}

function allowLocation() {
  Object.defineProperty(navigator, 'geolocation', {
    configurable: true,
    value: {
      getCurrentPosition: vi.fn((success) => success(POSITION))
    }
  })
}

function allowCamera(detectedToken = 'v1.k1.11.nonce.mac') {
  const stop = vi.fn()
  Object.defineProperty(navigator, 'mediaDevices', {
    configurable: true,
    value: {
      getUserMedia: vi.fn().mockResolvedValue({ getTracks: () => [{ stop }] })
    }
  })
  window.BarcodeDetector = class {
    detect() {
      return Promise.resolve([{ rawValue: detectedToken }])
    }
  }
  vi.spyOn(window.HTMLMediaElement.prototype, 'play').mockResolvedValue()
}

describe('WorkerScanView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useFakeTimers()
    scan.mockReset()
    allowLocation()
    delete window.BarcodeDetector
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
    delete window.BarcodeDetector
  })

  it('미지원 브라우저에서 QR Token 직접 입력을 제공하지 않는다', async () => {
    const wrapper = mountView()

    await wrapper.get('button').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('이 브라우저는 QR 스캔을 지원하지 않아요.')
    expect(wrapper.text()).not.toContain('QR 토큰 직접 입력')
    expect(wrapper.find('input').exists()).toBe(false)
  })

  it('카메라 QR과 현재 위치 전체를 새 멱등 의도로 전송한다', async () => {
    allowCamera()
    scan.mockResolvedValue({
      result: 'RECORDED',
      scanType: 'CHECK_IN',
      recordedAt: '2026-08-07T01:00:01Z',
      isLate: false,
      lateMinutes: 0,
      earlyCheckoutConfirmedAt: null,
      settlementDueAt: null
    })
    const wrapper = mountView()

    await wrapper.get('button').trigger('click')
    await flushPromises()
    await vi.advanceTimersByTimeAsync(350)
    await flushPromises()

    expect(scan).toHaveBeenCalledWith(
      {
        qrToken: 'v1.k1.11.nonce.mac',
        latitude: 37.1234568,
        longitude: 127.1234568,
        accuracyMeters: 12.35,
        capturedAt: '2026-08-07T01:00:00.000Z',
        confirmEarlyCheckout: false
      },
      { idempotencyKey: expect.any(String) }
    )
    expect(wrapper.text()).toContain('출근 처리되었습니다.')
  })

  it('조기 퇴근 확인은 현재 위치를 다시 측정하고 새 멱등 Key를 쓴다', async () => {
    allowCamera()
    scan
      .mockResolvedValueOnce({
        result: 'CONFIRMATION_REQUIRED',
        workCaseId: 17,
        scanType: 'CHECK_OUT',
        scheduledEndAt: '2026-08-07T09:00:00Z'
      })
      .mockResolvedValueOnce({
        result: 'RECORDED',
        workCaseId: 17,
        scanType: 'CHECK_OUT',
        recordedAt: '2026-08-07T01:00:02Z',
        isLate: false,
        lateMinutes: 0,
        earlyCheckoutConfirmedAt: '2026-08-07T01:00:02Z',
        settlementDueAt: '2026-08-08T01:00:02Z'
      })
    const wrapper = mountView()

    await wrapper.get('button').trigger('click')
    await flushPromises()
    await vi.advanceTimersByTimeAsync(350)
    await flushPromises()
    const confirmButton = wrapper
      .findAll('button')
      .find((button) => button.text().trim() === '퇴근 기록')
    await confirmButton.trigger('click')
    await flushPromises()

    expect(scan).toHaveBeenCalledTimes(2)
    expect(scan.mock.calls[1][0].confirmEarlyCheckout).toBe(true)
    expect(scan.mock.calls[1][1].idempotencyKey).not.toBe(scan.mock.calls[0][1].idempotencyKey)
    expect(navigator.geolocation.getCurrentPosition).toHaveBeenCalledTimes(2)
  })

  it('응답 유실 재확인은 최초 Body와 멱등 Key를 그대로 사용한다', async () => {
    allowCamera()
    scan.mockRejectedValueOnce(new Error('network')).mockResolvedValueOnce({
      result: 'RECORDED',
      scanType: 'CHECK_IN',
      recordedAt: '2026-08-07T01:00:01Z',
      isLate: false,
      lateMinutes: 0
    })
    const wrapper = mountView()

    await wrapper.get('button').trigger('click')
    await flushPromises()
    await vi.advanceTimersByTimeAsync(350)
    await flushPromises()

    expect(wrapper.text()).toContain('같은 요청 결과 다시 확인')
    const firstPayload = scan.mock.calls[0][0]
    const firstKey = scan.mock.calls[0][1].idempotencyKey

    await wrapper.get('button').trigger('click')
    await flushPromises()

    expect(scan).toHaveBeenCalledTimes(2)
    expect(scan.mock.calls[1][0]).toEqual(firstPayload)
    expect(scan.mock.calls[1][1].idempotencyKey).toBe(firstKey)
    expect(wrapper.text()).toContain('출근 처리되었습니다.')
  })
})
