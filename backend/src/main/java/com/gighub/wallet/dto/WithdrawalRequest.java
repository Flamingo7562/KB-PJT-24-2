package com.gighub.wallet.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gighub.bank.dto.BankInputNormalizer;
import lombok.Getter;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;

/** {bankCode, accountNo, amount}로 입금 대상 비귀속 Mock 계좌를 식별한다(DEC-WITHDRAWAL-DESTINATION). */
@Getter
public class WithdrawalRequest {

    @Pattern(regexp = "^(004|088|020|081|011)$", message = "지원하지 않는 bankCode입니다.")
    private final String bankCode;

    @Pattern(regexp = "^\\d{10,14}$", message = "accountNo는 10~14자리 숫자여야 합니다.")
    private final String accountNo;

    @NotNull
    @Positive
    @Max(100_000_000)
    private final Long amount;

    @JsonCreator
    public WithdrawalRequest(
            @JsonProperty("bankCode") String bankCode,
            @JsonProperty("accountNo") String accountNo,
            @JsonProperty("amount") Long amount) {
        this.bankCode = bankCode;
        this.accountNo = BankInputNormalizer.normalizeAccountNo(accountNo);
        this.amount = amount;
    }
}
