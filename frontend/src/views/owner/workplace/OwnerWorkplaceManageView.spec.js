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

import { listWorkplaces, updateWorkplace } from '@/services/workplaces'
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
  })
})
