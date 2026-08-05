package com.gighub.wallet.dto;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {bankCode, accountNo, amount} 입력 계약을 검증한다(DEC-WITHDRAWAL-DESTINATION). PIN은 없다. */
class WithdrawalRequestValidationTest {

    private static final String BANK_CODE = "004";
    private static final String ACCOUNT_NO = "1234567890";
    private static final Long AMOUNT = 100_000L;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidRequest() {
        assertTrue(validator.validate(request(BANK_CODE, ACCOUNT_NO, AMOUNT)).isEmpty());
    }

    @Test
    void acceptsEveryApprovedBankCode() {
        for (String bankCode : new String[]{"004", "088", "020", "081", "011"}) {
            assertTrue(
                    validator.validate(request(bankCode, ACCOUNT_NO, AMOUNT)).isEmpty(),
                    bankCode + "는 승인된 은행 코드여야 합니다."
            );
        }
    }

    @Test
    void rejectsUnapprovedBankCode() {
        assertViolates(request("999", ACCOUNT_NO, AMOUNT), "bankCode");
    }

    @Test
    void normalizesAccountNoBySpaceAndHyphenRemoval() {
        WithdrawalRequest request = request(BANK_CODE, "123-4567-890", AMOUNT);

        assertEquals(ACCOUNT_NO, request.getAccountNo());
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsAccountNoShorterThanTenDigits() {
        assertViolates(request(BANK_CODE, "123456789", AMOUNT), "accountNo");
    }

    @Test
    void rejectsAccountNoLongerThanFourteenDigits() {
        assertViolates(request(BANK_CODE, "123456789012345", AMOUNT), "accountNo");
    }

    @Test
    void rejectsNonDigitAccountNo() {
        assertViolates(request(BANK_CODE, "12345abc90", AMOUNT), "accountNo");
    }

    @Test
    void rejectsNullAmount() {
        assertViolates(request(BANK_CODE, ACCOUNT_NO, null), "amount");
    }

    @Test
    void rejectsZeroAndNegativeAmount() {
        assertViolates(request(BANK_CODE, ACCOUNT_NO, 0L), "amount");
        assertViolates(request(BANK_CODE, ACCOUNT_NO, -1L), "amount");
    }

    @Test
    void rejectsAmountAboveKrwCeiling() {
        assertViolates(request(BANK_CODE, ACCOUNT_NO, 100_000_001L), "amount");
    }

    @Test
    void acceptsAmountAtKrwCeiling() {
        assertTrue(validator.validate(request(BANK_CODE, ACCOUNT_NO, 100_000_000L)).isEmpty());
    }

    private void assertViolates(WithdrawalRequest request, String property) {
        Set<ConstraintViolation<WithdrawalRequest>> violations = validator.validate(request);
        assertTrue(
                violations.stream().anyMatch(
                        violation -> violation.getPropertyPath().toString().equals(property)
                ),
                () -> property + " 위반이 감지되어야 합니다. 실제 위반: " + violations
        );
    }

    private WithdrawalRequest request(String bankCode, String accountNo, Long amount) {
        return new WithdrawalRequest(bankCode, accountNo, amount);
    }
}
