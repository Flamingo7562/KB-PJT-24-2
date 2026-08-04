package com.gighub.badge.dto;

import lombok.Getter;

import java.util.List;
import java.util.Objects;

/**
 * 내 신뢰 뱃지 목록 응답입니다.
 *
 * <p>승인 명세의 최신 뱃지 응답은 {@code data}에 단일 뱃지 필드를 직접 담지만 현재 구현은
 * 목록을 반환합니다. 응답 필드 확정은 뱃지 산정 구현 범위이므로 이 클래스는 기존
 * {@code items} payload를 그대로 유지한 채 Envelope만 명시적 타입으로 바꿉니다.</p>
 *
 * <p>공개 Builder를 두면 {@code of()}의 방어 복사를 건너뛰고 호출자가 들고 있는 가변
 * 목록을 그대로 담을 수 있으므로, {@code PageResponse}처럼 생성자에서 복사하고 생성 경로를
 * {@code of()} 하나로 둡니다.</p>
 */
@Getter
public final class UserBadgeListResponse {

    private final List<UserBadge> items;

    private UserBadgeListResponse(List<UserBadge> items) {
        this.items = List.copyOf(Objects.requireNonNull(items, "items"));
    }

    public static UserBadgeListResponse of(List<UserBadge> items) {
        return new UserBadgeListResponse(items);
    }
}
