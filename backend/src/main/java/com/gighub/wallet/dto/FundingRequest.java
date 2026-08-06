package com.gighub.wallet.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gighub.bank.dto.BankInputNormalizer;
import lombok.Getter;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;

/** {bankCode, accountNo, pin, amount}로 비귀속 Mock 계좌를 식별해 충전한다(DEC-BANK-INPUT). */
@Getter
public class FundingRequest {

    // @Pattern은 null을 통과시키므로 필수 여부는 @NotBlank가 책임진다.
    @NotBlank(message = "bankCode는 필수입니다.")
    @Pattern(regexp = "^(004|088|020|081|011)$", message = "지원하지 않는 bankCode입니다.")
    private final String bankCode;

    @NotBlank(message = "accountNo는 필수입니다.")
    @Pattern(regexp = "^\\d{10,14}$", message = "accountNo는 10~14자리 숫자여야 합니다.")
    private final String accountNo;

    // 정규화·trim하지 않은 정확히 4자리 ASCII 숫자여야 한다(DEC-BANK-INPUT-VALIDATION).
    @NotBlank(message = "pin은 필수입니다.")
    @Pattern(regexp = "^[0-9]{4}$", message = "pin은 4자리 숫자여야 합니다.")
    private final String pin;

    @NotNull
    @Positive
    @Max(100_000_000)
    private final Long amount;

    @JsonCreator
    public FundingRequest(
            @JsonProperty("bankCode") String bankCode,
            @JsonProperty("accountNo") String accountNo,
            @JsonProperty("pin") String pin,
            @JsonProperty("amount") Long amount) {
        this.bankCode = bankCode;
        this.accountNo = BankInputNormalizer.normalizeAccountNo(accountNo);
        this.pin = pin;
        this.amount = amount;
    }
}
