package com.gighub.invitation.service;

import com.gighub.auth.security.AuthPrincipal;

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
}
