/**
 * 사업장 관리 화면 계약 테스트.
 * 승인 수정 허용 필드는 name, roadAddress, detailAddress, phone 이다(API_SPEC.md:370).
 * businessRegistrationNumber·representativeName·radiusMeters 를 보내면 400 이다.
 */
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }) }))
vi.mock('@/services/workplaces', () => ({
  listWorkplaces: vi.fn(),
  updateWorkplace: vi.fn(),
  deleteWorkplace: vi.fn()
}))
vi.mock('@/services/workCases', () => ({
  getWorkCaseSummary: vi.fn().mockResolvedValue({ inProgress: 0 })
}))
vi.mock('@/utils/daumPostcode', () => ({ embedAddressSearch: vi.fn() }))

import { deleteWorkplace, listWorkplaces, updateWorkplace } from '@/services/workplaces'
import { useUiStore } from '@/stores/ui'
import OwnerWorkplaceManageView from '@/views/owner/workplace/OwnerWorkplaceManageView.vue'

const WORKPLACE = {
  workplaceId: 1,
  businessRegistrationNumber: '1234567890',
  name: '강남점',
  representativeName: '김사장',
  roadAddress: '서울 강남구 테헤란로 1',
  detailAddress: '2층',
  phone: '0212345678',
  radiusMeters: 100,
  status: 'ACTIVE'
}

/** Teleport 를 stub 해 Modal 내용을 wrapper 안에서 찾을 수 있게 한다. */
function mountView() {
  return mount(OwnerWorkplaceManageView, { global: { stubs: { teleport: true } } })
}

function findByText(wrapper, selector, text) {
  const found = wrapper.findAll(selector).find((el) => el.text().trim() === text)
  if (!found) throw new Error(`'${text}' ${selector} 를 찾지 못했습니다`)
  return found
}

/** 첫 사업장의 '수정' 버튼을 눌러 다이얼로그를 연다. */
async function openEditDialog() {
  const wrapper = mountView()
  await flushPromises()
  await findByText(wrapper, 'button', '수정').trigger('click')
  await flushPromises()
  return wrapper
}

describe('OwnerWorkplaceManageView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    listWorkplaces.mockReset().mockResolvedValue({
      content: [{ ...WORKPLACE }],
      page: { number: 0, size: 100, totalElements: 1, totalPages: 1 }
    })
    updateWorkplace.mockReset().mockResolvedValue({ workplaceId: 1 })
    deleteWorkplace.mockReset().mockResolvedValue(undefined)
  })

  it('목록에 도로명과 세부주소를 함께 보여준다', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('.wp-address').text()).toContain('서울 강남구 테헤란로 1')
    expect(wrapper.find('.wp-address').text()).toContain('2층')
  })

  it('수정 다이얼로그는 도로명과 세부주소를 분리해 채운다', async () => {
    const wrapper = await openEditDialog()

    const values = wrapper.findAll('input').map((i) => i.element.value)
    expect(values).toContain('서울 강남구 테헤란로 1')
    expect(values).toContain('2층')
  })

  it('수정 요청은 승인 허용 필드만 보낸다', async () => {
    const wrapper = await openEditDialog()

    await findByText(wrapper, 'button', '저장').trigger('click')
    await flushPromises()

    const [, body] = updateWorkplace.mock.calls[0]
    expect(Object.keys(body).sort()).toEqual(
      ['detailAddress', 'name', 'phone', 'roadAddress'].sort()
    )
    // 키 집합만으로는 도로명·세부주소가 뒤바뀌어 전송돼도 잡히지 않는다 — 값도 각자 자리에 있는지 확인한다.
    expect(body.name).toBe(WORKPLACE.name)
    expect(body.roadAddress).toBe(WORKPLACE.roadAddress)
    expect(body.detailAddress).toBe(WORKPLACE.detailAddress)
    expect(body.phone).toBe('02-1234-5678') // WORKPLACE.phone('0212345678')이 화면 표시 형식으로 하이픈이 붙는다.
  })

  it('수정 성공 후 목록 갱신이 실패해도 실패로 보고하지 않는다', async () => {
    // GET /workplaces 가 일시적으로 실패해도 PATCH 자체는 이미 성공했다. 두 요청을
    // 같은 try 에 두면 갱신 실패가 수정 실패로 둔갑해 성공 토스트 대신 오류 토스트가 뜬다.
    const wrapper = await openEditDialog()
    const ui = useUiStore()
    const toastSpy = vi.spyOn(ui, 'toast')
    listWorkplaces.mockRejectedValueOnce(new Error('일시적인 네트워크 오류'))

    await findByText(wrapper, 'button', '저장').trigger('click')
    await flushPromises()

    expect(updateWorkplace).toHaveBeenCalledTimes(1)
    expect(toastSpy).toHaveBeenCalledWith('사업장 정보를 수정했어요.', { type: 'success' })
    expect(toastSpy).not.toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ type: 'danger' })
    )
  })

  it('삭제 성공 후 목록 갱신이 실패해도 실패로 보고하지 않는다', async () => {
    const wrapper = mountView()
    await flushPromises()
    const ui = useUiStore()
    const toastSpy = vi.spyOn(ui, 'toast')
    listWorkplaces.mockRejectedValueOnce(new Error('일시적인 네트워크 오류'))

    await findByText(wrapper, 'button', '삭제').trigger('click')
    await flushPromises()
    await findByText(wrapper, 'button', '삭제하기').trigger('click')
    await flushPromises()

    expect(deleteWorkplace).toHaveBeenCalledTimes(1)
    expect(toastSpy).toHaveBeenCalledWith('사업장을 삭제했어요.', { type: 'success' })
    expect(toastSpy).not.toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ type: 'danger' })
    )
  })
})
