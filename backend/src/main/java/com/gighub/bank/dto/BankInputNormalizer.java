package com.gighub.bank.dto;

/** Mock 계좌 요청 입력을 검증 전에 승인 형식으로 정규화합니다(DEC-BANK-INPUT-VALIDATION). */
public final class BankInputNormalizer {

    private BankInputNormalizer() {
    }

    public static String normalizeAccountNo(String value) {
        return value == null ? null : value.replaceAll("[\\s-]", "");
    }
}
