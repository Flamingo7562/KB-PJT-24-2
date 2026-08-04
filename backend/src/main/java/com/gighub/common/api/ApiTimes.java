package com.gighub.common.api;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * DB의 Asia/Seoul 기준 DATETIME 값을 API 시점 규약인 UTC Instant로 변환합니다.
 */
public final class ApiTimes {

    private static final ZoneId DATABASE_ZONE = ZoneId.of("Asia/Seoul");

    private ApiTimes() {
    }

    public static Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(DATABASE_ZONE).toInstant();
    }
}
