package com.gighub.invitation.controller;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.auth.security.AuthPrincipals;
import com.gighub.common.api.ApiResponse;
import com.gighub.invitation.dto.InvitationIssueResponse;
import com.gighub.invitation.service.InvitationIssueResult;
import com.gighub.invitation.service.InvitationIssueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 근무별 초대 Link 발급을 제공하는 Controller입니다.
 *
 * <p>요청 Body를 받지 않습니다. {@code @RequestBody} 자체를 두지 않아야 대상 사용자, 조건
 * Version, 만료, Token 같은 서버 결정 값을 호출자가 지정할 경로가 생기지 않습니다.</p>
 */
@RestController
public class WorkCaseInvitationController {

    private final InvitationIssueService invitationIssueService;

    public WorkCaseInvitationController(InvitationIssueService invitationIssueService) {
        this.invitationIssueService = invitationIssueService;
    }

    /**
     * 활성 초대 Link를 발급하거나 현재 Link를 그대로 반환합니다.
     *
     * <p>새 초대는 201, 이미 있던 활성 초대를 다시 돌려주는 경우는 200입니다. 두 응답의
     * Body는 같습니다.</p>
     */
    @PostMapping("/api/work-cases/{workCaseId}/invitations")
    public ResponseEntity<ApiResponse<InvitationIssueResponse>> issue(
            @PathVariable long workCaseId,
            Authentication authentication) {
        AuthPrincipal principal = AuthPrincipals.resolve(authentication);
        InvitationIssueResult result = invitationIssueService.issue(principal, workCaseId);

        return ResponseEntity
                .status(result.isCreated() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.of(result.getResponse()));
    }
}
