package com.gighub.common.api;

import com.gighub.common.exception.ValidationException;

/**
 * 보호 명세가 고정한 목록 Page Query 경계입니다.
 *
 * <p>기본값은 {@code page=0}, {@code size=20}이고 {@code size} 상한은 100입니다. 목록 API가
 * 각자 상수와 검증을 두면 Endpoint마다 허용 범위가 갈리므로 이 클래스 하나만 사용합니다.
 * Controller의 {@code @RequestParam defaultValue}에는 {@link #DEFAULT_PAGE_TEXT}와
 * {@link #DEFAULT_SIZE_TEXT}를 씁니다. Annotation 속성은 컴파일 상수만 받기 때문입니다.</p>
 */
public final class PageRequests {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public static final String DEFAULT_PAGE_TEXT = "0";
    public static final String DEFAULT_SIZE_TEXT = "20";

    private PageRequests() {
    }

    /**
     * 승인 Page 경계를 검증합니다. 위반하면 {@code 400 VALIDATION_ERROR}로 거부합니다.
     *
     * <p>상한을 넘는 {@code size}를 조용히 100으로 낮추지 않습니다. 요청과 다른 크기로
     * 응답하면 클라이언트가 Page 수를 잘못 계산합니다.</p>
     */
    public static void validate(int page, int size) {
        if (page < DEFAULT_PAGE) {
            throw new ValidationException("page는 0 이상이어야 합니다.");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new ValidationException("size는 1 이상 " + MAX_SIZE + " 이하여야 합니다.");
        }
    }

    /** 0-based Page 번호와 크기로 SQL OFFSET을 계산합니다. */
    public static long offset(int page, int size) {
        return (long) page * size;
    }

}
