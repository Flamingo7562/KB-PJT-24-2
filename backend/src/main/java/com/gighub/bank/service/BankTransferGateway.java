package com.gighub.bank.service;

/**
 * 외부 은행 이체 게이트웨이. 현재 DB Mock 구현
 * 전역 잠금 순서 : 지갑 -> 계좌
 */
public interface BankTransferGateway {
    // 계좌 소유권과 현재 사용 가능 상태를 사전 검증
    void preflight(BankAccountPreflightCommand command);

    // 계좌 -> 지갑 (충전)
    BankTransferResult withdraw(BankTransferCommand command);

    // 지갑 -> 계좌 (출금)
    BankTransferResult deposit(BankTransferCommand command);
}
