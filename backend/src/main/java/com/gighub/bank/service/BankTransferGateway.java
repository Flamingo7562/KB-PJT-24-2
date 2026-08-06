package com.gighub.bank.service;

/**
 * 외부 은행 이체 게이트웨이. 현재 DB Mock 구현
 * 전역 잠금 순서 : 지갑 -> 계좌
 */
public interface BankTransferGateway {
    // bankCode+accountNo로 비귀속 Mock 계좌의 내부 ID를 식별한다. 상태와 무관하게
    // 존재 자체만 확인하므로 Idempotency Replay가 계좌 현재 상태에 영향받지 않는다.
    Long resolveAccountId(String bankCode, String accountNo);

    // Mock Bank 내부 동시성은 이 Adapter 경계에서 직렬화한다. 로컬 주문의 계좌 FK가 계좌 Row에
    // S-lock을 건 뒤 이체 단계에서 X-lock으로 승격하면 같은 계좌를 쓰는 요청끼리 교착하므로,
    // 주문 선점 전에 X-lock을 먼저 잡는다. 상태·PIN은 검사하지 않는다(Replay 불변).
    void lockAccount(Long accountId);

    // 계좌 사용 가능 상태(및 충전 방향의 PIN)를 사전 검증
    void preflight(BankAccountPreflightCommand command);

    // 계좌 -> 지갑 (충전). 같은 트랜잭션에서 lockAccount(accountId)가 선행 호출되어
    // X-lock을 이미 쥐고 있어야 한다 - 중복 FOR UPDATE를 피하려고 비잠금 조회로 읽는다.
    BankTransferResult withdraw(BankTransferCommand command);

    // 지갑 -> 계좌 (출금). withdraw()와 같은 lockAccount() 선행 호출 전제를 공유한다.
    BankTransferResult deposit(BankTransferCommand command);
}
