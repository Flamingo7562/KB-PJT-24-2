package com.gighub.invitation.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 초대 설정 로딩과 값 검증 계약을 확인합니다.
 */
class InvitationPropertiesTest {

    private static final String SECRET = "local-invitation-secret-value-0123456789";
    private static final String PREVIOUS_SECRET = "rotated-invitation-secret-value-98765432";
    private static final String WEB_ORIGIN = "http://localhost:5173";

    @Test
    void readsInvitationKeysFromTheSharedExternalPropertyFile() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("invite.hmac.secret", SECRET)
                .withProperty("invite.web-origin", WEB_ORIGIN);

        InvitationProperties properties = new InvitationProperties(environment);

        assertEquals(WEB_ORIGIN, properties.getWebOrigin());
        assertFalse(properties.hasPreviousHmacSecret());
        assertNull(properties.getPreviousHmacSecret());
    }

    @Test
    void missingRequiredKeysFailFastAtStartup() {
        MockEnvironment withoutSecret = new MockEnvironment()
                .withProperty("invite.web-origin", WEB_ORIGIN);
        assertThrows(IllegalStateException.class, () -> new InvitationProperties(withoutSecret));

        MockEnvironment withoutOrigin = new MockEnvironment()
                .withProperty("invite.hmac.secret", SECRET);
        assertThrows(IllegalStateException.class, () -> new InvitationProperties(withoutOrigin));
    }

    @Test
    void blankPreviousSecretMeansNoRotationInProgress() {
        InvitationProperties properties = InvitationProperties.of(SECRET, "   ", WEB_ORIGIN);

        assertFalse(properties.hasPreviousHmacSecret());
    }

    @Test
    void rotationKeepsBothSecretsAvailable() {
        InvitationProperties properties =
                InvitationProperties.of(SECRET, PREVIOUS_SECRET, WEB_ORIGIN);

        assertTrue(properties.hasPreviousHmacSecret());
        assertArrayEquals(SECRET.getBytes(StandardCharsets.UTF_8), properties.getHmacSecret());
        assertArrayEquals(
                PREVIOUS_SECRET.getBytes(StandardCharsets.UTF_8),
                properties.getPreviousHmacSecret()
        );
    }

    @Test
    void shortSecretsAreRejectedWithoutEchoingTheValue() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> InvitationProperties.of("too-short-secret", null, WEB_ORIGIN)
        );

        assertFalse(exception.getMessage().contains("too-short-secret"));
        assertTrue(exception.getMessage().contains("invite.hmac.secret"));
    }

    @Test
    void webOriginMustBeAnAbsoluteOriginWithoutPathQueryOrFragment() {
        assertEquals(
                "https://app.example.com",
                InvitationProperties.of(SECRET, null, "https://app.example.com/").getWebOrigin()
        );

        assertThrows(IllegalStateException.class,
                () -> InvitationProperties.of(SECRET, null, "app.example.com"));
        assertThrows(IllegalStateException.class,
                () -> InvitationProperties.of(SECRET, null, "https://app.example.com/invitations"));
        assertThrows(IllegalStateException.class,
                () -> InvitationProperties.of(SECRET, null, "https://app.example.com?a=b"));
        assertThrows(IllegalStateException.class,
                () -> InvitationProperties.of(SECRET, null, "ftp://app.example.com"));
    }

    @Test
    void secretsAreNotExposedThroughToStringOrSharedArrays() {
        InvitationProperties properties =
                InvitationProperties.of(SECRET, PREVIOUS_SECRET, WEB_ORIGIN);

        assertFalse(properties.toString().contains(SECRET));
        assertFalse(properties.toString().contains(PREVIOUS_SECRET));
        assertTrue(properties.toString().contains(WEB_ORIGIN));

        // 호출부가 돌려받은 배열을 지워도 설정이 망가지지 않아야 합니다.
        byte[] borrowed = properties.getHmacSecret();
        Arrays.fill(borrowed, (byte) 0);
        assertArrayEquals(SECRET.getBytes(StandardCharsets.UTF_8), properties.getHmacSecret());
    }
}
