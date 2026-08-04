package com.gighub.wallet.dto;

import lombok.Builder;
import lombok.Value;

/**
 * 에스크로 예치 응답입니다.
 *
 * <p>승인 명세의 초대 수락 응답은 {@code workCaseId}와 {@code escrowStatus}이지만 현재
 * 구현은 안내 문구만 반환합니다. 응답 필드 확정은 초대 수락 계약을 정리하는 후속 범위이므로
 * 이 클래스는 기존 payload를 그대로 유지한 채 Envelope만 명시적 타입으로 바꿉니다.</p>
 */
@Value
@Builder
public class EscrowHoldResponse {

    String message;

    public static EscrowHoldResponse of(String message) {
        return EscrowHoldResponse.builder().message(message).build();
    }
}
