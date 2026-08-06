package com.gighub.auth.dto;

import java.util.Locale;

/** 인증 입력을 검증 전에 승인 형식으로 정규화합니다. */
public final class AuthNormalizer {

    private AuthNormalizer() {
    }

    static String normalizeIdentity(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    static String normalizeName(String value) {
        return value == null ? null : value.trim();
    }

    public static String normalizePhone(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("[\\s-]", "");
        return normalized.isEmpty() ? null : normalized;
    }
}
