package com.gighub.invitation.service;

import java.time.LocalDateTime;

/**
 * 수락 Transaction 안에서 OWNER의 임금을 예치 상태로 옮깁니다.
 *
 * <p>구형 에스크로 Spike(#22)의 {@code EscrowService}는 자기만의 잠금 순서로 근무를 잡고 근무
 * 상태 전이까지 직접 했습니다. 수락 Aggregate는 이미 근무와 초대를 정해진 순서로 잠근 뒤에
 * 도달하므로, 잠금과 상태 전이는 호출부가 책임지고 여기서는 <b>돈만</b> 다룹니다. 같은 행을
 * 두 곳에서 각자의 순서로 잠그면 교착이 생깁니다.</p>
 *
 * <p>구현은 지갑 도메인의 검증된 SQL을 그대로 씁니다. 조건부 잔액 이동과 원장 기록은 #60,
 * #120에서 다듬은 무결성 규칙이라 다시 쓰지 않습니다.</p>
 */
public interface AcceptEscrowHold {

    /**
     * 일급만큼 사용 가능 잔액을 잠금 잔액으로 옮기고 에스크로와 원장을 기록합니다.
     *
     * <p>호출 전에 근무 행과 초대 행이 잠겨 있어야 하며, OWNER 지갑은 이 Method가 잠급니다.
     * 잠금 순서의 마지막 단계입니다.</p>
     *
     * @param employerId 예치할 OWNER 식별자
     * @param workCaseId 대상 근무 식별자
     * @param amount     예치할 일급
     * @param claimId    멱등 Claim 식별자. 원장 Key를 이 값에서 파생합니다
     * @param acceptedAt Aggregate 전체가 공유하는 수락 시각
     * @return 생성된 에스크로 식별자
     */
    long hold(
            long employerId,
            long workCaseId,
            long amount,
            long claimId,
            LocalDateTime acceptedAt);
}
