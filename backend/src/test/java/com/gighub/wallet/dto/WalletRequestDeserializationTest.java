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
 * 모든 충전·출금·에스크로 요청이 런타임에서 조용히 깨진다. Controller 테스트가 이 세 DTO의
 * JSON 본문을 다루지 않으므로 역직렬화 계약을 여기서 직접 고정한다.</p>
 */
class WalletRequestDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void withdrawalRequestBindsJsonBody() throws Exception {
        WithdrawalRequest request = objectMapper.readValue(
                "{\"bankAccountId\":7,\"amount\":50000}",
                WithdrawalRequest.class
        );

        assertEquals(7L, request.getBankAccountId());
        assertEquals(50_000L, request.getAmount());
    }

    @Test
    void fundingRequestBindsJsonBody() throws Exception {
        FundingRequest request = objectMapper.readValue(
                "{\"bankAccountId\":3,\"amount\":1000}",
                FundingRequest.class
        );

        assertEquals(3L, request.getBankAccountId());
        assertEquals(1_000L, request.getAmount());
    }

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

            WithdrawalRequest empty =
                    objectMapper.readValue("{}", WithdrawalRequest.class);

            assertFalse(
                    validator.validate(empty).isEmpty(),
                    "빈 본문은 @NotNull 위반으로 검출돼야 한다"
            );
        }
    }
}
