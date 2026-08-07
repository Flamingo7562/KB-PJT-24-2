package com.gighub.invitation.service;

import com.gighub.invitation.dto.InvitationIssueResponse;

import java.util.Objects;

/**
 * 발급 결과와 그것이 새 초대인지 현재 Link 재조회인지를 함께 전달합니다.
 *
 * <p>두 경우의 Body는 같고 상태 코드만 201과 200으로 갈립니다. Service가 상태 코드를 직접
 * 정하면 HTTP 지식이 도메인으로 내려오고, Controller가 응답만 보고 추측하면 "이미 있던
 * Link"와 "방금 만든 Link"를 구분할 수 없습니다.</p>
 */
public final class InvitationIssueResult {

    private final InvitationIssueResponse response;
    private final boolean created;

    private InvitationIssueResult(InvitationIssueResponse response, boolean created) {
        this.response = Objects.requireNonNull(response, "response");
        this.created = created;
    }

    /** 새 초대를 만든 경우입니다. */
    public static InvitationIssueResult created(InvitationIssueResponse response) {
        return new InvitationIssueResult(response, true);
    }

    /** 유효한 활성 초대가 이미 있어 같은 Link를 그대로 돌려주는 경우입니다. */
    public static InvitationIssueResult existing(InvitationIssueResponse response) {
        return new InvitationIssueResult(response, false);
    }

    public InvitationIssueResponse getResponse() {
        return response;
    }

    public boolean isCreated() {
        return created;
    }
}
