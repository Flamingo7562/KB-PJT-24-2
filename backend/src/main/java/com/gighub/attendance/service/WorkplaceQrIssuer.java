package com.gighub.attendance.service;

/**
 * 사업장의 활성 고정 QR 한 건을 발급합니다.
 *
 * <p>사업장 생성과 재발급이 모두 이 단위를 씁니다. nonce 생성과 저장이 두 곳에 흩어지면
 * 한쪽만 고쳐지는 순간 발급 규칙이 갈라집니다. 사업장 도메인이 QR의 내부 구조를 알지
 * 않도록 호출 지점도 이 좁은 인터페이스 하나로 고정합니다.</p>
 */
public interface WorkplaceQrIssuer {

    /**
     * 사업장의 활성 고정 QR 한 건을 만듭니다.
     *
     * <p>호출자의 트랜잭션 안에서 실행되며, 같은 사업장에 이미 활성 QR이 있으면
     * {@code DuplicateKeyException}이 그대로 올라옵니다. 그것을 어떤 응답으로 바꿀지는 호출
     * 맥락이 정합니다.</p>
     *
     * @param workplaceId 대상 사업장 식별자
     * @param ownerUserId 그 사업장의 소유자. 복합 FK가 둘의 일치를 강제합니다
     * @return 저장한 nonce. 호출자는 이 값으로 응답 Token을 서명합니다
     */
    byte[] issueActive(Long workplaceId, Long ownerUserId);
}
