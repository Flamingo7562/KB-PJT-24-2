package com.gighub.attendance.qr;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 외부 properties에서 QR 서명 키를 읽는 계약을 검증합니다.
 *
 * <p>키가 없거나 약한 상태로 기동되면 QR 발급과 사업장 생성이 함께 깨집니다. 그 실패를 첫
 * 요청이 아니라 기동 시점으로 당기는 것이 이 단위의 목적입니다.</p>
 */
class QrHmacKeysTest {

    private static final String RAW_KEY_A = "01234567890123456789012345678901";
    private static final String RAW_KEY_B = "abcdefghijabcdefghijabcdefghijab";

    private static final String KEY_A = encode(RAW_KEY_A);
    private static final String KEY_B = encode(RAW_KEY_B);

    @Test
    void readsActiveKeyFromEnvironment() {
        QrHmacKeys keys = new QrHmacKeys(environment(KEY_A));

        assertEquals("k1", keys.activeKeyId());
        assertArrayEquals(RAW_KEY_A.getBytes(StandardCharsets.US_ASCII), keys.key("k1"));
    }

    @Test
    void keepsRetiredKeysForVerificationDuringRotation() {
        MockEnvironment environment = environment(KEY_A);
        environment.setProperty("qr.hmac.key-ids", "k1,k0");
        environment.setProperty("qr.hmac.key.k0", KEY_B);

        QrHmacKeys keys = new QrHmacKeys(environment);

        assertEquals("k1", keys.activeKeyId());
        assertArrayEquals(RAW_KEY_B.getBytes(StandardCharsets.US_ASCII), keys.key("k0"));
    }

    @Test
    void unknownKeyIdIsNull() {
        assertNull(new QrHmacKeys(environment(KEY_A)).key("nope"));
    }

    @Test
    void failsWhenActiveKeyIdIsMissing() {
        assertThrows(RuntimeException.class, () -> new QrHmacKeys(new MockEnvironment()));
    }

    @Test
    void failsWhenActiveKeyValueIsMissing() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("qr.hmac.active-key-id", "k1");

        assertThrows(RuntimeException.class, () -> new QrHmacKeys(environment));
    }

    @Test
    void failsWhenKeyIsShorterThanThirtyTwoBytes() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("qr.hmac.active-key-id", "k1");
        environment.setProperty("qr.hmac.key.k1", encode("tooshort"));

        assertThrows(IllegalStateException.class, () -> new QrHmacKeys(environment));
    }

    @Test
    void failsWhenKeyIsNotBase64() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("qr.hmac.active-key-id", "k1");
        environment.setProperty("qr.hmac.key.k1", "not base64!!");

        assertThrows(IllegalStateException.class, () -> new QrHmacKeys(environment));
    }

    @Test
    void failsWhenKeyIdShapeIsInvalid() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("qr.hmac.active-key-id", "key with space");
        environment.setProperty("qr.hmac.key.key with space", KEY_A);

        assertThrows(IllegalStateException.class, () -> new QrHmacKeys(environment));
    }

    private static MockEnvironment environment(String activeKey) {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("qr.hmac.active-key-id", "k1");
        environment.setProperty("qr.hmac.key.k1", activeKey);
        return environment;
    }

    private static String encode(String raw) {
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.US_ASCII));
    }
}
