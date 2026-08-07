package com.gighub.work.mapper.result;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 근무 상세의 최신 초대 한 건입니다.
 *
 * <p>초대 생명주기(발급·조회·수락)는 이 이슈의 범위가 아니라 초대 상태를 전용 enum으로
 * 두지 않고 DB 값을 그대로 담습니다. {@code termsVersion}은 컬럼명이 다른
 * {@code expected_terms_version}에서 옵니다(DEC-INVITE-LIFECYCLE 명칭 통일).</p>
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class LatestInvitationRow {

    private final String status;
    private final Integer termsVersion;
    private final LocalDateTime expiresAt;
}
