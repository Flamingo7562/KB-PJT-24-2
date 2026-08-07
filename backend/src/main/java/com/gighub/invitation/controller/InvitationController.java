package com.gighub.invitation.controller;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.auth.security.AuthPrincipals;
import com.gighub.common.api.ApiResponse;
import com.gighub.invitation.dto.InvitationDetailResponse;
import com.gighub.invitation.service.InvitationQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 초대 Link 조회를 제공하는 Controller입니다.
 *
 * <p>이 Endpoint는 공개 경로가 아니므로 비인증 요청은 공통 Security 경계에서
 * {@code 401 AUTH_REQUIRED}로 끝납니다. 역할 경계는 Service가 확인해
 * {@code 403 ROLE_MISMATCH}로 구분합니다.</p>
 *
 * <p>Frontend는 비로그인 웹 접근을 {@code /worker/login?redirect={encodedInvitationPath}}로
 * 보낸 뒤 같은 경로로 복귀합니다. 서버는 복귀 요청에서 Token과 상태를 다시 검증합니다.</p>
 */
@RestController
public class InvitationController {

    private final InvitationQueryService invitationQueryService;

    public InvitationController(InvitationQueryService invitationQueryService) {
        this.invitationQueryService = invitationQueryService;
    }

    /**
     * Token이 가리키는 초대의 근무 조건을 반환합니다.
     *
     * <p>Token은 경로 값으로만 받고 응답에 되돌려 담지 않습니다.</p>
     */
    @GetMapping("/api/invitations/{token}")
    public ResponseEntity<ApiResponse<InvitationDetailResponse>> findByToken(
            @PathVariable String token,
            Authentication authentication) {
        AuthPrincipal principal = AuthPrincipals.resolve(authentication);

        return ResponseEntity.ok(
                ApiResponse.of(invitationQueryService.findByToken(principal, token))
        );
    }
}
