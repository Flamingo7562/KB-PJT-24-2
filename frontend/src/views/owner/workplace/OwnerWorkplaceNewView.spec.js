/**
 * 사업장 등록 화면 계약 테스트.
 * needsWorkplaceSetup 은 서버가 요청 시점 DB 로 계산하는 값이다(API_SPEC.md:233).
 * 클라이언트가 등록 성공만 보고 false 로 단정하면, 서버가 아직 true 인 상태에서
 * OWNER 홈으로 보내 G7 가드와 무한 왕복이 생긴다.
 */
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const push = vi.fn()
vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))

vi.mock('@/services/workplaces', () => ({
  createWorkplace: vi.fn(),
  listWorkplaces: vi.fn()
}))
vi.mock('@/utils/daumPostcode', () => ({ embedAddressSearch: vi.fn() }))

import { createWorkplace, listWorkplaces } from '@/services/workplaces'
import { useAuthStore } from '@/stores/auth'
import OwnerWorkplaceNewView from '@/views/owner/workplace/OwnerWorkplaceNewView.vue'

// 화면은 setup 시점의 authStore.needsWorkplaceSetup 을 cameFromForcedSetup 으로 캡처한다.
// 따라서 setUser 는 반드시 mount 보다 먼저 호출해야 한다.
async function fillValidForm(wrapper) {
  const inputs = wrapper.findAll('input')
  // [0] 사업자등록번호 [1] 상호명 [2] 대표자명 [3] 도로명주소 [4] 세부주소 [5] 전화번호
  await inputs[0].setValue('1234567890')
  await inputs[1].setValue('강남점')
  await inputs[2].setValue('김사장')
  await inputs[3].setValue('서울 강남구 테헤란로 1')
  await inputs[4].setValue('2층')
  await inputs[5].setValue('02-1234-5678')
}

describe('OwnerWorkplaceNewView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    push.mockClear()
    createWorkplace.mockReset().mockResolvedValue({ workplaceId: 7 })
    listWorkplaces.mockReset().mockResolvedValue({
      content: [{ workplaceId: 7, name: '강남점', status: 'ACTIVE' }],
      page: { number: 0, size: 100, totalElements: 1, totalPages: 1 }
    })
  })

  it('등록 성공 후 세션을 다시 조회해 서버가 계산한 값을 쓴다', async () => {
    const auth = useAuthStore()
    auth.setUser({ name: '김사장', role: 'OWNER', needsWorkplaceSetup: true })
    const refreshSession = vi
      .spyOn(auth, 'refreshSession')
      .mockResolvedValue({ authenticated: true, role: 'OWNER', needsWorkplaceSetup: false })

    const wrapper = mount(OwnerWorkplaceNewView)
    await fillValidForm(wrapper)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(refreshSession).toHaveBeenCalledTimes(1)
    expect(push).toHaveBeenCalledWith('/owner/home')
  })

  it('서버가 여전히 설정이 필요하다고 하면 홈으로 보내지 않는다', async () => {
    const auth = useAuthStore()
    auth.setUser({ name: '김사장', role: 'OWNER', needsWorkplaceSetup: true })
    vi.spyOn(auth, 'refreshSession').mockResolvedValue({
      authenticated: true,
      role: 'OWNER',
      needsWorkplaceSetup: true
    })

    const wrapper = mount(OwnerWorkplaceNewView)
    await fillValidForm(wrapper)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(push).not.toHaveBeenCalledWith('/owner/home')
  })

  it('목록 갱신이 실패해도 등록 성공 흐름은 이어진다', async () => {
    // GET /api/workplaces 는 아직 서버에 없어 404 다(#145 후속). 목록 갱신 실패가
    // 전파되면 등록에 성공하고도 실패 토스트가 뜬다.
    listWorkplaces.mockRejectedValue(new Error('Request failed with status code 404'))
    const auth = useAuthStore()
    auth.setUser({ name: '김사장', role: 'OWNER', needsWorkplaceSetup: true })
    vi.spyOn(auth, 'refreshSession').mockResolvedValue({
      authenticated: true,
      role: 'OWNER',
      needsWorkplaceSetup: false
    })

    const wrapper = mount(OwnerWorkplaceNewView)
    await fillValidForm(wrapper)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(push).toHaveBeenCalledWith('/owner/home')
  })

  it('등록 Payload 는 승인 필드만 담고 radius 를 보내지 않는다', async () => {
    const auth = useAuthStore()
    auth.setUser({ name: '김사장', role: 'OWNER', needsWorkplaceSetup: true })
    vi.spyOn(auth, 'refreshSession').mockResolvedValue({
      authenticated: true,
      role: 'OWNER',
      needsWorkplaceSetup: false
    })

    const wrapper = mount(OwnerWorkplaceNewView)
    await fillValidForm(wrapper)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    const [payload] = createWorkplace.mock.calls[0]
    expect(Object.keys(payload).sort()).toEqual(
      [
        'businessRegistrationNumber',
        'detailAddress',
        'name',
        'representativeName',
        'roadAddress',
        'phone'
      ].sort()
    )
    expect(payload).not.toHaveProperty('radiusM')
    expect(payload).not.toHaveProperty('address')
  })
})
