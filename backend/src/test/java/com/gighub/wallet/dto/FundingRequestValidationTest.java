package com.gighub.wallet.dto;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {bankCode, accountNo, pin, amount} 입력 계약을 검증한다(DEC-BANK-INPUT-VALIDATION). */
class FundingRequestValidationTest {

    private static final String BANK_CODE = "004";
    private static final String ACCOUNT_NO = "1234567890";
    private static final String PIN = "0000";
    private static final Long AMOUNT = 100_000L;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidRequest() {
        assertTrue(validator.validate(request(BANK_CODE, ACCOUNT_NO, PIN, AMOUNT)).isEmpty());
    }

    @Test
    void acceptsEveryApprovedBankCode() {
        for (String bankCode : new String[]{"004", "088", "020", "081", "011"}) {
            assertTrue(
                    validator.validate(request(bankCode, ACCOUNT_NO, PIN, AMOUNT)).isEmpty(),
                    bankCode + "는 승인된 은행 코드여야 합니다."
            );
        }
    }

    @Test
    void rejectsUnapprovedBankCode() {
        assertViolates(request("999", ACCOUNT_NO, PIN, AMOUNT), "bankCode");
    }

    @Test
    void rejectsBankCodeAlias() {
        // 별칭("KB")은 API 값으로 허용하지 않는다.
        assertViolates(request("KB", ACCOUNT_NO, PIN, AMOUNT), "bankCode");
    }

    @Test
    void normalizesAccountNoBySpaceAndHyphenRemoval() {
        FundingRequest request = request(BANK_CODE, "123-4567-890", PIN, AMOUNT);

        assertEquals(ACCOUNT_NO, request.getAccountNo());
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsAccountNoShorterThanTenDigits() {
        assertViolates(request(BANK_CODE, "123456789", PIN, AMOUNT), "accountNo");
    }

    @Test
    void rejectsAccountNoLongerThanFourteenDigits() {
        assertViolates(request(BANK_CODE, "123456789012345", PIN, AMOUNT), "accountNo");
    }

    @Test
    void rejectsNonDigitAccountNo() {
        assertViolates(request(BANK_CODE, "12345abc90", PIN, AMOUNT), "accountNo");
    }

    @Test
    void doesNotTrimOrNormalizePin() {
        // trim했다면 " 0000"(5자)이 유효한 "0000"(4자)이 됐겠지만, pin은 trim하지 않으므로
        // 여전히 5자여서 4자리 숫자 패턴을 벗어나 거부된다.
        assertViolates(request(BANK_CODE, ACCOUNT_NO, " 0000", AMOUNT), "pin");
    }

    @Test
    void rejectsPinShorterThanFourDigits() {
        assertViolates(request(BANK_CODE, ACCOUNT_NO, "000", AMOUNT), "pin");
    }

    @Test
    void rejectsPinLongerThanFourDigits() {
        assertViolates(request(BANK_CODE, ACCOUNT_NO, "00000", AMOUNT), "pin");
    }

    @Test
    void rejectsNonDigitPin() {
        assertViolates(request(BANK_CODE, ACCOUNT_NO, "00a0", AMOUNT), "pin");
    }

    @Test
    void rejectsNullAmount() {
        assertViolates(request(BANK_CODE, ACCOUNT_NO, PIN, null), "amount");
    }

    @Test
    void rejectsZeroAndNegativeAmount() {
        assertViolates(request(BANK_CODE, ACCOUNT_NO, PIN, 0L), "amount");
        assertViolates(request(BANK_CODE, ACCOUNT_NO, PIN, -1L), "amount");
    }

    @Test
    void rejectsAmountAboveKrwCeiling() {
        assertViolates(request(BANK_CODE, ACCOUNT_NO, PIN, 100_000_001L), "amount");
    }

    @Test
    void acceptsAmountAtKrwCeiling() {
        assertTrue(
                validator.validate(request(BANK_CODE, ACCOUNT_NO, PIN, 100_000_000L)).isEmpty()
        );
    }

    private void assertViolates(FundingRequest request, String property) {
        Set<ConstraintViolation<FundingRequest>> violations = validator.validate(request);
        assertTrue(
                violations.stream().anyMatch(
                        violation -> violation.getPropertyPath().toString().equals(property)
                ),
                () -> property + " 위반이 감지되어야 합니다. 실제 위반: " + violations
        );
    }

    private FundingRequest request(String bankCode, String accountNo, String pin, Long amount) {
        return new FundingRequest(bankCode, accountNo, pin, amount);
    }
}
