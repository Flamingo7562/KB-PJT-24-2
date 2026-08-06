package com.gighub.wallet.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 지갑 요청 DTO는 Builder 기반으로 역직렬화된다.
 *
 * <p>{@code @Builder}만 붙이고 {@code @Jacksonized}를 빠뜨리면 Jackson이 필드를 채우지 못해
 * 요청이 런타임에서 조용히 깨진다. FundingRequest/WithdrawalRequest는 bankCode·accountNo·pin
 * 계약(DEC-BANK-INPUT)으로 재설계되며 Builder 대신 명시적 {@code @JsonCreator} 생성자로
 * 바뀌었고, 그 역직렬화·검증 계약은 FundingRequestValidationTest/WithdrawalRequestValidationTest,
 * FundingControllerTest/WithdrawalControllerTest가 다룬다.</p>
 */
class WalletRequestDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void escrowHoldRequestBindsJsonBody() throws Exception {
        EscrowHoldRequest request = objectMapper.readValue(
                "{\"employerId\":1,\"workerId\":2,\"workCaseId\":3,\"amount\":4}",
                EscrowHoldRequest.class
        );

        assertEquals(1L, request.getEmployerId());
        assertEquals(2L, request.getWorkerId());
        assertEquals(3L, request.getWorkCaseId());
        assertEquals(4L, request.getAmount());
    }

    @Test
    void missingFieldsStillFailBeanValidation() throws Exception {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            EscrowHoldRequest empty =
                    objectMapper.readValue("{}", EscrowHoldRequest.class);

            assertFalse(
                    validator.validate(empty).isEmpty(),
                    "빈 본문은 @NotNull 위반으로 검출돼야 한다"
            );
        }
    }
}
