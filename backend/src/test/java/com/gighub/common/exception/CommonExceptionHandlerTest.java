package com.gighub.common.exception;

import com.gighub.wallet.exception.EscrowAccessDeniedException;
import com.gighub.wallet.exception.InvalidFundingRequestException;
import com.gighub.wallet.exception.InvalidIdempotencyKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommonExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingController())
                .setControllerAdvice(new CommonExceptionHandler())
                .build();
    }

    @Test
    void invalidIdempotencyKeyReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/test/invalid-key"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IDEMPOTENCY_KEY"));
    }

    @Test
    void invalidFundingRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/test/invalid-funding"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void escrowOwnershipFailureReturnsForbidden() throws Exception {
        mockMvc.perform(get("/test/escrow-forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("WORK_CASE_FORBIDDEN"));
    }

    @Test
    void unexpectedDuplicateReturnsInternalError() throws Exception {
        mockMvc.perform(get("/test/unexpected-duplicate"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @RestController
    private static class ThrowingController {

        @GetMapping("/test/invalid-key")
        public void invalidKey() {
            throw new InvalidIdempotencyKeyException("invalid");
        }

        @GetMapping("/test/invalid-funding")
        public void invalidFunding() {
            throw new InvalidFundingRequestException("invalid");
        }

        @GetMapping("/test/escrow-forbidden")
        public void escrowForbidden() {
            throw new EscrowAccessDeniedException("forbidden");
        }

        @GetMapping("/test/unexpected-duplicate")
        public void unexpectedDuplicate() {
            throw new DuplicateKeyException("unexpected");
        }
    }
}
