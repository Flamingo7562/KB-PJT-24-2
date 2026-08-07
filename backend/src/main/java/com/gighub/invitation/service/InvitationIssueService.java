package com.gighub.invitation.service;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.invitation.dto.InvitationIssueResponse;

/** 근무 초대 Link 발급 진입점입니다. */
public interface InvitationIssueService {

    /**
     * 미매칭 {@code DRAFT} 근무의 활성 초대 Link를 발급하거나 현재 Link를 그대로 돌려줍니다.
     *
     * <p>대상 사용자·조건 Version·만료·Token은 모두 서버가 정합니다. 호출자는 어떤 값도
     * 지정할 수 없습니다.</p>
     *
     * @param principal  인증 Principal
     * @param workCaseId 발급 대상 근무 식별자
     * @return 새 초대이면 {@code created}, 기존 활성 초대이면 {@code existing}
     */
    InvitationIssueResult issue(AuthPrincipal principal, long workCaseId);

    /**
     * 현재 활성 초대를 철회하고 다른 ID·Token의 새 초대로 교체합니다.
     *
     * <p>발급과 달리 항상 새 Link를 만듭니다. 교체 전 Link는 즉시 철회 상태가 되므로, 이미
     * 공유한 Link를 무효화하려는 경우에만 사용합니다. 유효한 활성 초대가 없으면 교체할
     * 대상이 없어 충돌로 끝납니다.</p>
     *
     * <p>멱등 Replay를 제공하지 않습니다. 동시 요청은 요청마다 Link를 교체하며 마지막 Link만
     * 유효합니다. 응답이 유실됐을 때는 재발급을 다시 시도하지 말고 일반 발급으로 현재 Link를
     * 조회해야 합니다.</p>
     *
     * @param principal  인증 Principal
     * @param workCaseId 대상 근무 식별자
     * @return 새로 만든 초대의 Link와 만료
     */
    InvitationIssueResponse reissue(AuthPrincipal principal, long workCaseId);
}
