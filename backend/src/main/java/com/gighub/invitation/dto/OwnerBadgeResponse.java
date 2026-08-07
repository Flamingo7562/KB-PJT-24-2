package com.gighub.invitation.dto;

import lombok.Getter;

/**
 * 초대한 OWNER의 현재 승인 Badge입니다.
 *
 * <p>활성 Badge가 없으면 이 객체 대신 {@code ownerBadge: null}을 반환합니다. 빈 객체나 기본
 * 등급으로 채우면 받는 쪽이 "Badge 없음"과 "가장 낮은 Badge"를 구분할 수 없습니다.</p>
 */
@Getter
public final class OwnerBadgeResponse {

    private final String badgeType;
    private final Integer level;

    private OwnerBadgeResponse(String badgeType, Integer level) {
        this.badgeType = badgeType;
        this.level = level;
    }

    public static OwnerBadgeResponse of(String badgeType, Integer level) {
        return new OwnerBadgeResponse(badgeType, level);
    }
}
