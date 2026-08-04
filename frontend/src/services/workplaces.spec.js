/**
 * 사업장 서비스 계약 테스트 — 전화번호 저장 형식을 고정한다.
 * 승인 계약(DEC-PHONE-STORAGE)은 `workplaces.phone` 을 `users.phone` 과 독립된 원천으로 두되
 * 같은 정규화 형식을 쓰도록 정한다. 화면은 하이픈 표시를 유지하므로 전송 직전에 정규화한다.
 */
import { describe, expect, it } from 'vitest'

import { createWorkplace, listWorkplaces, updateWorkplace } from '@/services/workplaces'

describe('createWorkplace', () => {
  it('전화번호를 구분 문자 없는 숫자로 정규화해 보관한다', async () => {
    const { workplaceId } = await createWorkplace({
      name: '정규화 테스트점',
      address: '서울 어딘가 1',
      phone: '02-1234-5678'
    })
    const created = (await listWorkplaces()).find((w) => w.workplaceId === workplaceId)
    expect(created).toBeDefined()

    const updated = await updateWorkplace(workplaceId, { phone: '010-1111-2222' })
    expect(updated.phone).toBe('01011112222')
  })
})

describe('updateWorkplace', () => {
  it('하이픈이 섞인 전화번호를 숫자만 남겨 보낸다', async () => {
    const { workplaceId } = await createWorkplace({ name: '수정 테스트점', phone: '0212345678' })
    const updated = await updateWorkplace(workplaceId, { phone: '02 9876 5432' })
    expect(updated.phone).toBe('0298765432')
  })

  it('phone 을 보내지 않는 부분 수정은 phone 키를 만들지 않는다', async () => {
    const { workplaceId } = await createWorkplace({ name: '부분 수정점', phone: '021112222' })
    const updated = await updateWorkplace(workplaceId, { name: '이름만 변경' })
    expect(updated).not.toHaveProperty('phone')
    expect(updated.name).toBe('이름만 변경')
  })
})
