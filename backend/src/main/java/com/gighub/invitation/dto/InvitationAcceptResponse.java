package com.gighub.invitation.dto;

import lombok.Getter;

/**
 * 초대 수락 결과입니다.
 *
 * <p>계약 식별자, 문서 식별자, 금액, 당사자 정보는 담지 않습니다. 수락 직후 필요한 것은
 * "어느 근무가 확정됐고 임금이 예치됐는가" 두 가지뿐이고, 나머지는 근무 상세 조회가
 * 권한 검사를 거쳐 제공합니다.</p>
 */
@Getter
public final class InvitationAcceptResponse {

    private final long workCaseId;

    /** 예치 결과입니다. 수락이 성공하면 항상 {@code HELD}입니다. */
    private final String escrowStatus;

    private InvitationAcceptResponse(long workCaseId, String escrowStatus) {
        this.workCaseId = workCaseId;
        this.escrowStatus = escrowStatus;
    }

    public static InvitationAcceptResponse held(long workCaseId) {
        return new InvitationAcceptResponse(workCaseId, "HELD");
    }

    public static InvitationAcceptResponse of(long workCaseId, String escrowStatus) {
        return new InvitationAcceptResponse(workCaseId, escrowStatus);
    }
}
