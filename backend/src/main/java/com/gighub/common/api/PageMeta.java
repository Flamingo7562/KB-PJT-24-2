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
     * <p>{@code totalPages}는 전체 건수를 Page 크기로 올림한 값이며, 결과가 없으면 0입니다.
     * {@code size}는 {@link PageRequests#validate(int, int)}가 1 이상을 보장합니다.</p>
     */
    public static PageMeta of(int number, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageMeta(number, size, totalElements, totalPages);
    }

}
