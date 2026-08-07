package com.gighub.attendance.qr;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * QR Token 서명에 쓰는 외부 HMAC 키를 보관합니다.
 *
 * <p>키는 저장소와 WAR에 포함하지 않고 {@code DatabaseConfig}가 읽는 것과 같은 외부
 * properties에서 주입합니다. 값이 없거나 형식이 어긋나면 기동을 중단합니다. 키 없이 뜬
 * 서버는 첫 QR 요청에서야 실패하고, 그 시점에는 이미 사업장 생성이 함께 깨집니다.</p>
 *
 * <p>Token은 서명에 쓴 키 식별자를 담습니다. 이 QR은 인쇄되어 매장에 부착되므로 키를
 * 교체할 때 구 키를 검증용으로 남겨두어야 이미 배포된 인쇄물이 계속 동작합니다.</p>
 */
@Component
public class QrHmacKeys {

    /**
     * HMAC-SHA256의 Block 크기 미만인 키는 강도를 보장하지 않으므로 32바이트를 하한으로 둡니다.
     */
    private static final int MIN_KEY_LENGTH = 32;

    private static final Pattern KEY_ID = Pattern.compile("\\A[A-Za-z0-9_-]{1,16}\\z");

    private static final String ACTIVE_KEY_ID_PROPERTY = "qr.hmac.active-key-id";
    private static final String KEY_IDS_PROPERTY = "qr.hmac.key-ids";
    private static final String KEY_PROPERTY_PREFIX = "qr.hmac.key.";

    private final String activeKeyId;
    private final Map<String, byte[]> keys;

    /**
     * 생성자가 둘이라 Container가 어느 쪽을 쓸지 스스로 정하지 못합니다. 주입 대상을 명시하지
     * 않으면 기본 생성자를 찾다가 기동이 깨집니다.
     */
    @Autowired
    public QrHmacKeys(Environment environment) {
        this(readActiveKeyId(environment), readKeys(environment));
    }

    /** 테스트가 properties 없이 키를 직접 넣을 때 쓰는 생성자입니다. */
    QrHmacKeys(String activeKeyId, Map<String, byte[]> keys) {
        this.activeKeyId = requireValidKeyId(activeKeyId);
        this.keys = Map.copyOf(keys);
        if (!this.keys.containsKey(this.activeKeyId)) {
            throw new IllegalStateException(
                    "활성 QR 서명 키 " + this.activeKeyId + "의 값이 없습니다.");
        }
    }

    public String activeKeyId() {
        return activeKeyId;
    }

    /** 등록되지 않은 키 식별자는 {@code null}입니다. 호출자가 검증 실패로 처리합니다. */
    public byte[] key(String keyId) {
        byte[] value = keys.get(keyId);
        return value == null ? null : value.clone();
    }

    private static String readActiveKeyId(Environment environment) {
        return environment.getRequiredProperty(ACTIVE_KEY_ID_PROPERTY).trim();
    }

    /**
     * 검증에 쓸 키 목록을 읽습니다.
     *
     * <p>{@code qr.hmac.key-ids}를 생략하면 활성 키 하나만 등록합니다. 목록이 둘 이상인
     * 상태는 키 교체 기간뿐입니다.</p>
     */
    private static Map<String, byte[]> readKeys(Environment environment) {
        String activeKeyId = readActiveKeyId(environment);
        String configured = environment.getProperty(KEY_IDS_PROPERTY, activeKeyId);

        Map<String, byte[]> decoded = new LinkedHashMap<>();
        for (String rawKeyId : configured.split(",")) {
            String keyId = requireValidKeyId(rawKeyId.trim());
            decoded.put(keyId, decodeKey(
                    environment.getRequiredProperty(KEY_PROPERTY_PREFIX + keyId).trim(), keyId));
        }
        return decoded;
    }

    private static byte[] decodeKey(String encoded, String keyId) {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "QR 서명 키 " + keyId + "는 Base64여야 합니다.", exception);
        }
        if (key.length < MIN_KEY_LENGTH) {
            throw new IllegalStateException(
                    "QR 서명 키 " + keyId + "는 " + MIN_KEY_LENGTH + "바이트 이상이어야 합니다.");
        }
        return key;
    }

    private static String requireValidKeyId(String keyId) {
        if (keyId == null || !KEY_ID.matcher(keyId).matches()) {
            throw new IllegalStateException(
                    "QR 서명 키 식별자는 1~16자의 영숫자, '-', '_'만 사용합니다: " + keyId);
        }
        return keyId;
    }
}
