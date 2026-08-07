/** 공통 오류 Envelope에서 승인 오류 Code를 읽는다. */
export function invitationErrorCode(error) {
  return error?.response?.data?.code ?? error?.code ?? ''
}

/** 초대 조회·수락 오류를 승인 Code와 현재 재확인 상태에 맞는 사용자 문구로 바꾼다. */
export function invitationErrorMessage(error, { sameIntentRetry = false } = {}) {
  const code = invitationErrorCode(error)
  switch (code) {
    case 'RESOURCE_NOT_FOUND':
      return '초대 링크를 찾을 수 없어요.'
    case 'INVITATION_EXPIRED':
      return '초대 링크가 만료됐어요. 사장님께 새 링크를 요청해주세요.'
    case 'INVITATION_REVOKED':
      return '철회된 초대 링크예요. 사장님께 새 링크를 요청해주세요.'
    case 'INVITATION_ALREADY_ACCEPTED':
      return '이미 수락된 초대 링크예요.'
    case 'INVITATION_TERMS_CHANGED':
      return '근무 조건이 변경됐어요. 새 조건으로 발급된 링크를 확인해주세요.'
    case 'WORK_CASE_LOCKED':
      return '현재 상태에서는 이 근무를 확정할 수 없어요.'
    case 'IDEMPOTENCY_KEY_REUSED':
      return '요청 정보가 바뀌었어요. 화면을 새로고침한 뒤 다시 시도해주세요.'
    case 'ROLE_MISMATCH':
    case 'FORBIDDEN':
      return '이 초대를 확인할 권한이 없어요.'
    case 'CONFLICT':
      return sameIntentRetry
        ? '확정 결과를 아직 확인하지 못했어요. 같은 요청으로 다시 확인해주세요.'
        : '사장님이 임금을 예치할 수 없어 지금은 근무를 확정할 수 없어요.'
    case 'INTERNAL_ERROR':
      return '확정 결과를 확인하지 못했어요. 같은 요청으로 다시 확인해주세요.'
    default:
      return '초대 정보를 처리하지 못했어요. 잠시 후 다시 시도해주세요.'
  }
}

/** 서버 Commit 여부가 불확실한 요청과 그 동일 의도 재확인에서만 Key를 유지한다. */
export function shouldRetainAcceptanceKey(error, { requestWasUncertain = false } = {}) {
  const status = error?.response?.status
  const code = invitationErrorCode(error)

  // 승인 계약은 잔액 부족과 처리 중 Claim을 같은 CONFLICT로 묶는다. 서버 문구를 파싱하지 않고,
  // 이미 결과가 불확실했던 같은 의도를 재확인하는 경우에만 안전한 Replay Key를 보존한다.
  const retryableConflict = code === 'CONFLICT' && requestWasUncertain
  return status === undefined || status >= 500 || retryableConflict
}

export function isInvitationForbidden(error) {
  const code = invitationErrorCode(error)
  return error?.response?.status === 403 || code === 'ROLE_MISMATCH' || code === 'FORBIDDEN'
}
