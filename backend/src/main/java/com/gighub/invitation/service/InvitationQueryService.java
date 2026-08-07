package com.gighub.invitation.service;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.invitation.dto.InvitationDetailResponse;

/** 초대 Link로 근무 조건을 읽는 진입점입니다. */
public interface InvitationQueryService {

    /**
     * Token이 가리키는 초대의 근무 조건을 조회합니다.
     *
     * <p>인증된 WORKER에게만 조건을 돌려주며, 사용할 수 없는 초대는 상태별 승인 오류로
     * 끝납니다. 실패 응답에는 초대 내용과 OWNER Badge가 포함되지 않습니다.</p>
     *
     * @param principal 인증 Principal
     * @param token     요청 경로의 Token 원문
     * @return 승인 명세가 확정한 근무 조건
     */
    InvitationDetailResponse findByToken(AuthPrincipal principal, String token);
}
