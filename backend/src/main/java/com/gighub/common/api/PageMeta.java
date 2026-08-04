package com.gighub.common.api;

import lombok.Getter;

/**
 * 보호 명세가 고정한 목록 Page Metadata입니다.
 *
 * <p>필드는 {@code number}, {@code size}, {@code totalElements}, {@code totalPages}
 * 넷으로 고정합니다. 명세에 없는 {@code first}, {@code last}, {@code hasNext} 같은
 * 파생 필드를 구현 편의로 추가하지 않습니다.</p>
 */
@Getter
public final class PageMeta {

    private final int number;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    private PageMeta(int number, int size, long totalElements, int totalPages) {
        this.number = number;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    /**
     * 0-based Page 번호와 Page 크기, 전체 건수로 Metadata를 만듭니다.
     *
     * <p>{@code totalPages}는 전체 건수를 Page 크기로 올림한 값이며, 결과가 없으면 0입니다.</p>
     *
     * <p>Controller가 {@link PageRequests#validate(int, int)}를 호출한다는 전제를 여기서 다시
     * 확인합니다. {@code size}가 0이면 실수 나눗셈이 예외 없이 Infinity나 NaN이 되어
     * {@code totalPages}가 {@code Integer.MAX_VALUE} 또는 0으로 조용히 응답에 실립니다.
     * 요청 경계 위반은 Controller에서 {@code 400}으로 거부하고, 검증을 건너뛴 호출은 여기서
     * 프로그래밍 오류로 끊습니다. 상한 {@code size <= 100}은 요청 규칙이므로 반복하지 않습니다.</p>
     *
     * @throws IllegalArgumentException {@code number}가 음수이거나 {@code size}가 1 미만인 경우
     */
    public static PageMeta of(int number, int size, long totalElements) {
        if (number < 0 || size < 1) {
            throw new IllegalArgumentException(
                    "PageMeta는 number 0 이상, size 1 이상에서만 만들 수 있습니다. "
                            + "Controller가 PageRequests.validate를 먼저 호출해야 합니다.");
        }

        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageMeta(number, size, totalElements, totalPages);
    }

}
