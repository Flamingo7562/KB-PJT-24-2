package com.gighub.bank.service;

/**
 * 외부 은행 이체 게이트웨이. 현재 DB Mock 구현
 * 전역 잠금 순서 : 지갑 -> 계좌
 */
public interface BankTransferGateway {
    // bankCode+accountNo로 비귀속 Mock 계좌의 내부 ID를 식별한다. 상태와 무관하게
    // 존재 자체만 확인하므로 Idempotency Replay가 계좌 현재 상태에 영향받지 않는다.
    Long resolveAccountId(String bankCode, String accountNo);

    // 계좌 사용 가능 상태(및 충전 방향의 PIN)를 사전 검증
    void preflight(BankAccountPreflightCommand command);

    // 계좌 -> 지갑 (충전)
    BankTransferResult withdraw(BankTransferCommand command);

    // 지갑 -> 계좌 (출금)
    BankTransferResult deposit(BankTransferCommand command);
}
