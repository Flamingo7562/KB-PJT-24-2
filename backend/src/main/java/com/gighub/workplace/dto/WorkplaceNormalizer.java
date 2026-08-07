package com.gighub.workplace.dto;

/** 사업장 전용 텍스트 입력을 검증 전에 승인 형식으로 정규화합니다. */
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
}
