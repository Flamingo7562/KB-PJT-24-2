package com.gighub.invitation.mapper.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 초대 한 건의 조회 결과입니다.
 *
 * <p>{@code status}는 DB 값을 그대로 담습니다. 허용 값은 {@code ck_work_invitations_status}가
 * {@code PENDING}, {@code ACCEPTED}, {@code REJECTED}, {@code REVOKED}, {@code EXPIRED}로
 * 제한합니다.</p>
 *
 * <p>{@code tokenHash}만 담고 Token 원문 필드는 두지 않습니다. 저장소에 원문이 없으므로 이
 * 행에도 담을 값이 없고, 필드를 만들어 두면 이후 계층이 응답이나 로그에 실을 수 있습니다.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationRow {

    private Long id;
    private Long workCaseId;
    private byte[] tokenHash;
    private String status;
    private Integer expectedTermsVersion;
    private LocalDateTime expiresAt;
    private Long acceptedByUserId;
}
