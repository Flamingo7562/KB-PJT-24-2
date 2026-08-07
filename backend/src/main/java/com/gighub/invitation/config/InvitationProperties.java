package com.gighub.invitation.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 초대 Token 파생 Secret과 초대 Link 주소를 외부 설정에서 읽습니다.
 *
 * <p>Secret은 WAR와 Git에 포함하지 않고 {@code gighub.database.config}로 전달한 외부
 * properties 파일에서만 읽습니다. 값이 없으면 Token을 만들 수 없으므로 기동 시점에
 * 실패시킵니다.</p>
 *
 * <p>Key 교체 중에는 이전 Secret으로 만든 활성 초대의 Link를 계속 재현해야 하므로
 * {@code invite.hmac.previous-secret}을 선택 값으로 둡니다.</p>
 */
@Component
public class InvitationProperties {

    /** Base64url Token 43자와 SHA-256 Hash를 감당할 최소 Key 길이입니다. */
    private static final int MINIMUM_SECRET_LENGTH = 32;

    private static final String SECRET_KEY = "invite.hmac.secret";
    private static final String PREVIOUS_SECRET_KEY = "invite.hmac.previous-secret";
    private static final String WEB_ORIGIN_KEY = "invite.web-origin";

    private final byte[] hmacSecret;
    private final byte[] previousHmacSecret;
    private final String webOrigin;

    // 테스트용 정적 팩터리와 생성자가 함께 있으므로 주입 대상 생성자를 명시합니다.
    @Autowired
    public InvitationProperties(Environment environment) {
        this(
                environment.getRequiredProperty(SECRET_KEY),
                environment.getProperty(PREVIOUS_SECRET_KEY),
                environment.getRequiredProperty(WEB_ORIGIN_KEY)
        );
    }

    private InvitationProperties(String secret, String previousSecret, String webOrigin) {
        this.hmacSecret = toSecretBytes(SECRET_KEY, secret);
        this.previousHmacSecret = previousSecret == null || previousSecret.isBlank()
                ? null
                : toSecretBytes(PREVIOUS_SECRET_KEY, previousSecret);
        this.webOrigin = normalizeWebOrigin(webOrigin);
    }

    /** 테스트가 Spring 환경 없이 같은 검증 규칙으로 설정을 만들 때 사용합니다. */
    public static InvitationProperties of(String secret, String previousSecret, String webOrigin) {
        return new InvitationProperties(secret, previousSecret, webOrigin);
    }

    /** 새 초대 Token을 만들 때 사용하는 현재 Secret입니다. */
    public byte[] getHmacSecret() {
        return hmacSecret.clone();
    }

    /** Key 교체 중 이전 Secret으로 만든 Token을 재현할 때만 사용합니다. */
    public byte[] getPreviousHmacSecret() {
        return previousHmacSecret == null ? null : previousHmacSecret.clone();
    }

    public boolean hasPreviousHmacSecret() {
        return previousHmacSecret != null;
    }

    /** Query·Fragment와 끝 슬래시가 없는 초대 Link Origin입니다. */
    public String getWebOrigin() {
        return webOrigin;
    }

    private static byte[] toSecretBytes(String key, String value) {
        Objects.requireNonNull(value, key);
        String trimmed = value.trim();
        if (trimmed.length() < MINIMUM_SECRET_LENGTH) {
            // 예외 메시지에 Secret 값을 넣지 않고 설정 Key와 요구 길이만 알립니다.
            throw new IllegalStateException(
                    key + "는 " + MINIMUM_SECRET_LENGTH + "자 이상이어야 합니다."
            );
        }
        return trimmed.getBytes(StandardCharsets.UTF_8);
    }

    private static String normalizeWebOrigin(String value) {
        String trimmed = Objects.requireNonNull(value, WEB_ORIGIN_KEY).trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        URI uri = URI.create(trimmed);
        boolean absoluteHttpOrigin = uri.isAbsolute()
                && ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                && uri.getHost() != null;
        boolean pathFree = (uri.getPath() == null || uri.getPath().isEmpty())
                && uri.getQuery() == null
                && uri.getFragment() == null;
        if (!absoluteHttpOrigin || !pathFree) {
            throw new IllegalStateException(
                    WEB_ORIGIN_KEY + "는 경로·Query·Fragment 없는 절대 http(s) Origin이어야 합니다."
            );
        }
        return trimmed;
    }

    /** Secret이 로그·오류·디버거 출력에 노출되지 않도록 값을 숨깁니다. */
    @Override
    public String toString() {
        return "InvitationProperties{webOrigin=" + webOrigin
                + ", hmacSecret=****"
                + ", previousHmacSecret=" + (previousHmacSecret == null ? "none" : "****")
                + "}";
    }
}
