package com.gighub.common.validation;

/** 인증과 사업장 전화번호 입력이 공유하는 정규화 및 형식 계약입니다. */
public final class PhoneNormalizer {

    /** 공백과 하이픈을 제거한 뒤 허용하는 국내 전화번호 형식입니다. */
    public static final String VALID_PATTERN = "^0\\d{8,10}$";

    private PhoneNormalizer() {
    }

    /** 표시용 공백과 하이픈을 제거하고, 구분 문자만 남은 입력은 없는 값으로 다룹니다. */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("[\\s-]", "");
        return normalized.isEmpty() ? null : normalized;
    }
}
