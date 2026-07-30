package com.gighub.auth;

/**
 * 로그인 아이디·이메일·전화번호·이름 정규화 규칙. 중복확인·가입·로그인(후속 브랜치)에서
 * 동일하게 적용해야 하므로 한 곳에 모아둔다.
 */
public final class AuthNormalizer {

    private AuthNormalizer() {
    }

    public static String loginId(String value) {
        return value == null ? null : value.trim();
    }

    public static String email(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    public static String phone(String value) {
        if (value == null) {
            return null;
        }
        String digitsOnly = value.replaceAll("[-\\s]", "");
        return digitsOnly.isEmpty() ? null : digitsOnly;
    }

    public static String name(String value) {
        return value == null ? null : value.trim();
    }
}
