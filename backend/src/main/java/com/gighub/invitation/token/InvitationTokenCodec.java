package com.gighub.invitation.token;

import com.gighub.invitation.config.InvitationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

/**
 * 초대 Token 원문을 초대 ID에서 파생하고 저장용 Hash로 변환합니다.
 *
 * <p>저장소에는 SHA-256 Hash만 남기므로 무작위 Token은 한 번 응답한 뒤 다시 만들 수
 * 없습니다. 활성 초대의 같은 Link를 다시 돌려주려면 원문을 재현할 수 있어야 하므로,
 * Secret과 초대 ID로 결정론적으로 파생합니다.</p>
 *
 * <p>Token 원문은 반환값으로만 전달하며 이 클래스는 어떤 경우에도 원문을 로그나 예외
 * 메시지에 남기지 않습니다.</p>
 */
@Component
public class InvitationTokenCodec {

    private static final String MAC_ALGORITHM = "HmacSHA256";
    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * 파생 입력에 붙이는 고정 표지입니다.
     *
     * <p>같은 Secret이 다른 용도로 재사용되더라도 초대 Token과 값이 겹치지 않게 합니다.</p>
     */
    private static final String DERIVATION_LABEL = "gighub:invitation-token:v1:";

    /** 32byte를 Base64url 무패딩으로 인코딩한 길이입니다. */
    private static final int TOKEN_LENGTH = 43;

    /** SHA-256 Hash 길이이며 {@code work_invitations.token_hash}의 BINARY(32)와 같습니다. */
    public static final int TOKEN_HASH_LENGTH = 32;

    private final InvitationProperties properties;

    public InvitationTokenCodec(InvitationProperties properties) {
        this.properties = properties;
    }

    /**
     * 현재 Secret으로 초대 ID의 Token 원문을 파생합니다.
     *
     * @param invitationId 저장된 초대 행의 식별자
     * @return Base64url 무패딩 43자 Token 원문
     */
    public String deriveToken(long invitationId) {
        return deriveToken(invitationId, properties.getHmacSecret());
    }

    /**
     * 저장된 Hash와 일치하는 Token 원문을 재현합니다.
     *
     * <p>Key 교체 중에는 이전 Secret으로 만든 활성 초대가 남아 있으므로 현재 Secret으로 먼저
     * 시도하고, 일치하지 않으면 이전 Secret으로 한 번 더 시도합니다. 어느 쪽과도 맞지 않으면
     * 그 초대의 Link는 재현할 수 없습니다.</p>
     *
     * @param invitationId 저장된 초대 행의 식별자
     * @param storedTokenHash 저장된 Token Hash
     * @return 재현에 성공한 Token 원문
     */
    public Optional<String> reproduceToken(long invitationId, byte[] storedTokenHash) {
        String currentToken = deriveToken(invitationId, properties.getHmacSecret());
        if (matches(currentToken, storedTokenHash)) {
            return Optional.of(currentToken);
        }
        if (!properties.hasPreviousHmacSecret()) {
            return Optional.empty();
        }

        String previousToken = deriveToken(invitationId, properties.getPreviousHmacSecret());
        return matches(previousToken, storedTokenHash)
                ? Optional.of(previousToken)
                : Optional.empty();
    }

    /**
     * Token 원문을 저장·조회용 SHA-256 Hash로 변환합니다.
     *
     * @param token Token 원문
     * @return BINARY(32)에 대응하는 32byte Hash
     */
    public byte[] hash(String token) {
        if (token == null) {
            throw new IllegalArgumentException("token");
        }
        try {
            return MessageDigest.getInstance(HASH_ALGORITHM)
                    .digest(token.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }

    /**
     * Token 원문이 저장된 Hash와 같은지 상수 시간으로 비교합니다.
     *
     * @param token Token 원문
     * @param storedTokenHash 저장된 Token Hash
     * @return 두 Hash가 같으면 {@code true}
     */
    public boolean matches(String token, byte[] storedTokenHash) {
        if (token == null || storedTokenHash == null) {
            return false;
        }
        // 앞자리 비교로 끝나지 않도록 길이가 달라도 전체를 비교하는 isEqual을 사용합니다.
        return MessageDigest.isEqual(hash(token), storedTokenHash);
    }

    /**
     * 요청 경로의 Token이 파생 결과와 같은 형식인지 확인합니다.
     *
     * <p>형식이 다른 값은 저장소를 조회하지 않고 미존재와 같은 응답으로 처리하기 위한
     * 판별이며, 형식만으로 초대의 존재 여부를 알려 주지 않습니다.</p>
     *
     * @param token 요청이 전달한 Token 문자열
     * @return Base64url 무패딩 43자 형식이면 {@code true}
     */
    public boolean isWellFormed(String token) {
        if (token == null || token.length() != TOKEN_LENGTH) {
            return false;
        }
        for (int index = 0; index < token.length(); index++) {
            char character = token.charAt(index);
            boolean allowed = (character >= 'A' && character <= 'Z')
                    || (character >= 'a' && character <= 'z')
                    || (character >= '0' && character <= '9')
                    || character == '-'
                    || character == '_';
            if (!allowed) {
                return false;
            }
        }
        return true;
    }

    private static String deriveToken(long invitationId, byte[] secret) {
        if (invitationId <= 0L) {
            throw new IllegalArgumentException("invitationId");
        }
        try {
            Mac mac = Mac.getInstance(MAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, MAC_ALGORITHM));
            byte[] derived = mac.doFinal(
                    (DERIVATION_LABEL + invitationId).getBytes(StandardCharsets.UTF_8)
            );
            return Base64.getUrlEncoder().withoutPadding().encodeToString(derived);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("초대 Token을 파생할 수 없습니다.", exception);
        }
    }
}
