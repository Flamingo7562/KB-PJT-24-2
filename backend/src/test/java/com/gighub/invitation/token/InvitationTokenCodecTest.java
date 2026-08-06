package com.gighub.invitation.token;

import com.gighub.invitation.config.InvitationProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 초대 Token 파생·Hash 변환 계약을 검증합니다.
 */
class InvitationTokenCodecTest {

    private static final String CURRENT_SECRET = "current-invitation-secret-value-0123456789";
    private static final String PREVIOUS_SECRET = "previous-invitation-secret-value-98765432";
    private static final String WEB_ORIGIN = "http://localhost:5173";

    private final InvitationTokenCodec codec = new InvitationTokenCodec(
            InvitationProperties.of(CURRENT_SECRET, null, WEB_ORIGIN)
    );

    @Test
    void tokenIsBase64UrlWithoutPaddingAndStableForTheSameInvitation() {
        String first = codec.deriveToken(41L);
        String second = codec.deriveToken(41L);

        // 활성 초대에 같은 Link를 다시 돌려주려면 파생이 결정론적이어야 합니다.
        assertEquals(first, second);
        assertEquals(43, first.length());
        assertTrue(codec.isWellFormed(first));
        assertFalse(first.contains("="));
        assertFalse(first.contains("+"));
        assertFalse(first.contains("/"));
    }

    @Test
    void differentInvitationsAndSecretsProduceDifferentTokens() {
        Set<String> tokens = new HashSet<>();
        for (long invitationId = 1L; invitationId <= 200L; invitationId++) {
            tokens.add(codec.deriveToken(invitationId));
        }
        assertEquals(200, tokens.size());

        InvitationTokenCodec otherKeyCodec = new InvitationTokenCodec(
                InvitationProperties.of(PREVIOUS_SECRET, null, WEB_ORIGIN)
        );
        assertNotEquals(codec.deriveToken(41L), otherKeyCodec.deriveToken(41L));
    }

    @Test
    void hashIsSha256OfTokenAndFitsBinary32() {
        String token = codec.deriveToken(41L);
        byte[] hash = codec.hash(token);

        assertEquals(InvitationTokenCodec.TOKEN_HASH_LENGTH, hash.length);
        assertTrue(MessageDigest.isEqual(sha256(token), hash));
    }

    @Test
    void matchesComparesHashesAndRejectsMismatch() {
        String token = codec.deriveToken(41L);
        byte[] storedHash = codec.hash(token);

        assertTrue(codec.matches(token, storedHash));
        assertFalse(codec.matches(codec.deriveToken(42L), storedHash));
        assertFalse(codec.matches(token, null));
        assertFalse(codec.matches(null, storedHash));
        // 길이가 다른 값을 넣어도 예외 없이 불일치로 끝나야 합니다.
        assertFalse(codec.matches(token, new byte[]{1, 2, 3}));
    }

    @Test
    void reproduceTokenFallsBackToPreviousSecretDuringRotation() {
        InvitationTokenCodec beforeRotation = new InvitationTokenCodec(
                InvitationProperties.of(PREVIOUS_SECRET, null, WEB_ORIGIN)
        );
        byte[] storedHash = beforeRotation.hash(beforeRotation.deriveToken(41L));

        InvitationTokenCodec afterRotation = new InvitationTokenCodec(
                InvitationProperties.of(CURRENT_SECRET, PREVIOUS_SECRET, WEB_ORIGIN)
        );
        Optional<String> reproduced = afterRotation.reproduceToken(41L, storedHash);

        assertTrue(reproduced.isPresent());
        assertEquals(beforeRotation.deriveToken(41L), reproduced.get());
    }

    @Test
    void reproduceTokenReturnsEmptyWhenNoConfiguredSecretMatches() {
        byte[] unrelatedHash = codec.hash("unrelated-token-value");

        assertTrue(codec.reproduceToken(41L, unrelatedHash).isEmpty());

        InvitationTokenCodec rotated = new InvitationTokenCodec(
                InvitationProperties.of(CURRENT_SECRET, PREVIOUS_SECRET, WEB_ORIGIN)
        );
        assertTrue(rotated.reproduceToken(41L, unrelatedHash).isEmpty());
    }

    @Test
    void reproduceTokenPrefersCurrentSecret() {
        InvitationTokenCodec rotated = new InvitationTokenCodec(
                InvitationProperties.of(CURRENT_SECRET, PREVIOUS_SECRET, WEB_ORIGIN)
        );
        byte[] currentHash = rotated.hash(codec.deriveToken(41L));

        assertEquals(codec.deriveToken(41L), rotated.reproduceToken(41L, currentHash).orElseThrow());
    }

    @Test
    void malformedTokensAreRejectedBeforeStorageLookup() {
        assertFalse(codec.isWellFormed(null));
        assertFalse(codec.isWellFormed(""));
        assertFalse(codec.isWellFormed("short"));
        assertFalse(codec.isWellFormed(codec.deriveToken(41L) + "x"));
        assertFalse(codec.isWellFormed(codec.deriveToken(41L).substring(0, 42) + "!"));
        assertFalse(codec.isWellFormed(codec.deriveToken(41L).substring(0, 42) + "/"));
    }

    @Test
    void invalidInvitationIdsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> codec.deriveToken(0L));
        assertThrows(IllegalArgumentException.class, () -> codec.deriveToken(-1L));
        assertThrows(IllegalArgumentException.class, () -> codec.hash(null));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
