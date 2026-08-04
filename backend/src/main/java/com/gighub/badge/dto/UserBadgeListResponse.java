package com.gighub.badge.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 내 신뢰 뱃지 목록 응답입니다.
 *
 * <p>승인 명세의 최신 뱃지 응답은 {@code data}에 단일 뱃지 필드를 직접 담지만 현재 구현은
 * 목록을 반환합니다. 응답 필드 확정은 뱃지 산정 구현 범위이므로 이 클래스는 기존
 * {@code items} payload를 그대로 유지한 채 Envelope만 명시적 타입으로 바꿉니다.</p>
 */
@Value
@Builder
public class UserBadgeListResponse {

    List<UserBadge> items;

    public static UserBadgeListResponse of(List<UserBadge> items) {
        return UserBadgeListResponse.builder().items(List.copyOf(items)).build();
    }
}
