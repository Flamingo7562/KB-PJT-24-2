package com.gighub.invitation.dto;

import lombok.Getter;

import java.time.Instant;

/**
 * 초대 발급과 현재 Link 재조회의 공통 응답입니다.
 *
 * <p>Token 원문은 이 {@code inviteUrl} 안에서만 밖으로 나갑니다. 저장소에는 Hash만 남기므로
 * 별도 {@code token} 필드를 두지 않습니다.</p>
 *
 * <p>{@code expiresAt}은 근무 시작 시각과 같습니다. 근무가 시작된 뒤에 수락된 초대는 의미가
 * 없어 만료 기준을 따로 두지 않습니다.</p>
 */
@Getter
public final class InvitationIssueResponse {

    private final String inviteUrl;
    private final Instant expiresAt;

    private InvitationIssueResponse(String inviteUrl, Instant expiresAt) {
        this.inviteUrl = inviteUrl;
        this.expiresAt = expiresAt;
    }

    public static InvitationIssueResponse of(String inviteUrl, Instant expiresAt) {
        return new InvitationIssueResponse(inviteUrl, expiresAt);
    }
}
