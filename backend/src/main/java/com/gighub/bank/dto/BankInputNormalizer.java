package com.gighub.bank.dto;

/** Mock 계좌 요청 입력을 검증 전에 승인 형식으로 정규화합니다(DEC-BANK-INPUT-VALIDATION). */
public final class BankInputNormalizer {

    /** SPEC 4.1.0 승인 canonical bankCode 20종(docs/specs/API_SPEC.md '지갑과 거래'). */
    public static final String BANK_CODE_PATTERN =
            "^(004|088|020|081|011|003|090|092|089|032|031|131|034|023|027|002|007|045|048|071)$";

    private BankInputNormalizer() {
    }

    public static String normalizeAccountNo(String value) {
        return value == null ? null : value.replaceAll("[\\s-]", "");
    }
}
