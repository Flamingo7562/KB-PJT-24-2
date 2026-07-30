package com.gighub.wallet.idempotency;

import com.gighub.wallet.exception.InvalidIdempotencyKeyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletIdempotencyKeysTest {

    private static final String RAW_KEY = "shared-key-001";

    @Test
    @DisplayName("같은 원문 키도 자금 흐름 범위별로 서로 다른 69자 ASCII 키가 된다")
    void createsDistinctFixedLengthKeysByScope() {
        Set<String> keys = Set.of(
                WalletIdempotencyKeys.funding(RAW_KEY),
                WalletIdempotencyKeys.escrowHold(RAW_KEY),
                WalletIdempotencyKeys.escrowReleaseEmployer(RAW_KEY),
                WalletIdempotencyKeys.escrowReleaseWorker(RAW_KEY),
                WalletIdempotencyKeys.withdrawal(RAW_KEY)
        );

        assertEquals(5, keys.size());
        keys.forEach(key -> {
            assertEquals(69, key.length());
            assertEquals(key.length(), key.getBytes(StandardCharsets.US_ASCII).length);
            assertTrue(key.matches("[A-Z]{4}:[0-9a-f]{64}"));
        });
    }

    @Test
    @DisplayName("원문이 다른 범위의 인코딩 형태를 흉내 내도 다시 안전하게 해시한다")
    void hashesPrefixLikeRawKeyAgain() {
        String prefixLikeRawKey = WalletIdempotencyKeys.funding(RAW_KEY);

        assertTrue(
                !prefixLikeRawKey.equals(
                        WalletIdempotencyKeys.escrowHold(prefixLikeRawKey)
                )
        );
    }

    @Test
    @DisplayName("빈 값, 공백, 비 ASCII, 100자 초과 키를 거부한다")
    void rejectsInvalidRawKeys() {
        assertThrows(
                InvalidIdempotencyKeyException.class,
                () -> WalletIdempotencyKeys.funding("")
        );
        assertThrows(
                InvalidIdempotencyKeyException.class,
                () -> WalletIdempotencyKeys.funding("contains space")
        );
        assertThrows(
                InvalidIdempotencyKeyException.class,
                () -> WalletIdempotencyKeys.funding("한글")
        );
        assertThrows(
                InvalidIdempotencyKeyException.class,
                () -> WalletIdempotencyKeys.funding("a".repeat(101))
        );
    }
}
