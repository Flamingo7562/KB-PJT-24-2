package com.gighub.common.api;

import lombok.Getter;

import java.util.List;
import java.util.Objects;

/**
 * 보호 명세의 목록 성공 응답 payload입니다.
 *
 * <p>{@link ApiResponse}의 {@code data}에 담겨 {@code {data:{content,page}}} 구조가 됩니다.
 * 목록 API는 {@code ApiResponse.of(PageResponse.of(...))} 형태로 반환합니다.</p>
 *
 * @param <T> 목록 Item 타입
 */
@Getter
public final class PageResponse<T> {

    private final List<T> content;
    private final PageMeta page;

    private PageResponse(List<T> content, PageMeta page) {
        this.content = List.copyOf(Objects.requireNonNull(content, "content"));
        this.page = Objects.requireNonNull(page, "page");
    }

    public static <T> PageResponse<T> of(List<T> content, PageMeta page) {
        return new PageResponse<>(content, page);
    }

    /** 0-based Page 번호, Page 크기와 전체 건수로 Metadata까지 함께 만듭니다. */
    public static <T> PageResponse<T> of(List<T> content, int number, int size, long totalElements) {
        return new PageResponse<>(content, PageMeta.of(number, size, totalElements));
    }

}
