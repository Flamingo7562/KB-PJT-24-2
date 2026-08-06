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

    /**
     * 전화번호만 구분 문자를 제거합니다.
     *
     * <p>승인 명세가 정규화 대상으로 열거한 입력에만 적용합니다. 사업자등록번호는 그 목록에
     * 없으므로 같은 규칙을 확대 적용하지 않습니다. 표시 형식을 서버가 받아 주면 명세대로면
     * 거절될 요청이 통과해 입력 계약이 조용히 넓어집니다.</p>
     */
    static String normalizePhone(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("[\\s-]", "");
        return normalized.isEmpty() ? null : normalized;
    }
}
