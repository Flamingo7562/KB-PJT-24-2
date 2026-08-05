package com.gighub.workplace.dto;

/**
 * 사업장 입력을 검증 전에 승인 형식으로 정규화합니다.
 *
 * <p>인증 Package의 정규화 유틸은 Package 전용이라 재사용할 수 없어 사업장 계약에 필요한
 * 규칙만 여기에 둡니다. 전화번호 규칙은 {@code users.phone}과 독립된
 * {@code workplaces.phone}에도 같은 숫자 문자열 저장 계약을 적용하기 위해 동일하게
 * 맞춥니다.</p>
 */
final class WorkplaceNormalizer {

    private WorkplaceNormalizer() {
    }

    static String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    /** 공백만 남는 선택 입력은 DB의 빈 문자열 금지 CHECK와 맞추기 위해 없는 값으로 다룹니다. */
    static String normalizeOptionalText(String value) {
        String normalized = normalizeText(value);
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }

    static String normalizePhone(String value) {
        return stripSeparators(value);
    }

    /**
     * 화면은 {@code 123-45-67890} 표시 형식을 쓰지만 DB는 숫자 10자리만 허용하므로
     * 전화번호와 같은 구분 문자 제거 규칙을 적용합니다.
     */
    static String normalizeBusinessRegistrationNumber(String value) {
        return stripSeparators(value);
    }

    private static String stripSeparators(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("[\\s-]", "");
        return normalized.isEmpty() ? null : normalized;
    }
}
