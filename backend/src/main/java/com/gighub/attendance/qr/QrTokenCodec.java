package com.gighub.attendance.qr;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

/**
 * 사업장 고정 QR Token을 서명하고 검증합니다.
 *
 * <p>DB와 Spring 상태를 알지 않는 순수 단위입니다. 서명 불변식을 컨테이너 없이 검증할 수
 * 있고, 근태 스캔 구현이 검증 쪽만 그대로 가져다 쓸 수 있습니다.</p>
 *
 * <p>Token은 {@code v1}, 키 식별자, 사업장 식별자, nonce, MAC 다섯 세그먼트를 마침표로 이은
 * 문자열입니다. MAC은 앞 네 세그먼트 문자열 전체를 덮으므로 어느 세그먼트를 바꿔도 검증이
 * 실패합니다.</p>
 */
@Component
public class QrTokenCodec {

    public static final int NONCE_LENGTH = 16;

    private static final String VERSION = "v1";
    private static final String ALGORITHM = "HmacSHA256";
    private static final String SEPARATOR = ".";
    private static final int SEGMENT_COUNT = 5;

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final QrHmacKeys keys;

    public QrTokenCodec(QrHmacKeys keys) {
        this.keys = keys;
    }

    public String sign(long workplaceId, byte[] nonce) {
        if (nonce == null || nonce.length != NONCE_LENGTH) {
            throw new IllegalArgumentException("QR nonce는 " + NONCE_LENGTH + "바이트여야 합니다.");
        }
        String keyId = keys.activeKeyId();
        String signed = signedPart(keyId, workplaceId, ENCODER.encodeToString(nonce));
        return signed + SEPARATOR + ENCODER.encodeToString(mac(keyId, signed));
    }

    /**
     * 검증에 성공하면 Token이 담고 있던 값을 돌려줍니다.
     *
     * <p>구조 오류, 모르는 키 식별자, MAC 불일치를 모두 빈 결과로 합칩니다. 실패 원인을 나눠
     * 알려주면 호출자에게 어느 단계까지 통과했는지 알려주게 됩니다.</p>
     */
    public Optional<QrTokenPayload> verify(String token) {
        if (token == null) {
            return Optional.empty();
        }
        String[] segments = token.split("\\" + SEPARATOR, -1);
        if (segments.length != SEGMENT_COUNT || !VERSION.equals(segments[0])) {
            return Optional.empty();
        }

        long workplaceId;
        byte[] nonce;
        byte[] presented;
        try {
            workplaceId = Long.parseLong(segments[2]);
            nonce = DECODER.decode(segments[3]);
            presented = DECODER.decode(segments[4]);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        if (nonce.length != NONCE_LENGTH || keys.key(segments[1]) == null) {
            return Optional.empty();
        }

        byte[] expected = mac(segments[1], signedPart(segments[1], workplaceId, segments[3]));
        if (!MessageDigest.isEqual(expected, presented)) {
            return Optional.empty();
        }
        return Optional.of(new QrTokenPayload(workplaceId, nonce));
    }

    private static String signedPart(String keyId, long workplaceId, String encodedNonce) {
        return VERSION + SEPARATOR + keyId + SEPARATOR + workplaceId + SEPARATOR + encodedNonce;
    }

    private byte[] mac(String keyId, String signedPart) {
        byte[] key = keys.key(keyId);
        if (key == null) {
            throw new IllegalStateException("등록되지 않은 QR 서명 키입니다: " + keyId);
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            return mac.doFinal(signedPart.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("QR Token 서명에 실패했습니다.", exception);
        }
    }
}
