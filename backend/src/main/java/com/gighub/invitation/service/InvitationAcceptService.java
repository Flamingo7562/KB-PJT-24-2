package com.gighub.invitation.service;

import com.gighub.auth.security.AuthPrincipal;

/** Body 없는 초대 수락 진입점입니다. */
public interface InvitationAcceptService {

    /**
     * 초대를 수락해 매칭·계약·임금 예치·정산 예약을 한 번에 확정합니다.
     *
     * <p>당사자·근무·조건 Version·금액은 모두 Token과 인증 Principal에서 서버가 도출합니다.
     * 호출자는 어떤 값도 전달하지 않습니다.</p>
     *
     * @param principal 인증 Principal
     * @param token     요청 경로의 Token 원문
     * @param rawKey    {@code Idempotency-Key} Header 원문
     * @return 최초 성공이면 {@code first}, 저장된 결과 재생이면 {@code replayed}
     */
    InvitationAcceptResult accept(AuthPrincipal principal, String token, String rawKey);
}
