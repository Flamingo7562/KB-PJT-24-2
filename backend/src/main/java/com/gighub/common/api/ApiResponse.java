package com.gighub.common.api;

import lombok.Getter;

import java.util.Objects;

/**
 * 보호 명세의 단일 성공 응답 {@code {data}} Envelope입니다.
 *
 * <p>Controller가 응답을 {@code Map}으로 조립하면 필드 이름 오타나 누락을 컴파일 단계에서
 * 잡지 못하고 Runtime Schema에도 실제 구조가 드러나지 않습니다. 성공 응답은 이 타입으로
 * 감싸고 payload는 도메인 DTO를 사용합니다.</p>
 *
 * <p>{@code 204 No Content}처럼 Body가 없는 응답과 파일 Stream, 정적 자원은 이 Envelope로
 * 감싸지 않습니다.</p>
 *
 * @param <T> 승인 명세가 정의한 payload 타입
 */
@Getter
public final class ApiResponse<T> {

    private final T data;

    private ApiResponse(T data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data);
    }

}
