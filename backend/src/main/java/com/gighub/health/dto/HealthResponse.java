package com.gighub.health.dto;

import java.time.Instant;

/**
 * Health API가 반환하는 읽기 전용 응답입니다.
 *
 * @param service 확인 대상 서비스 이름
 * @param status 현재 서비스 상태
 * @param checkedAt 서버가 상태를 확인한 UTC 기준 시각
 */
public record HealthResponse(String service, String status, Instant checkedAt) {
}

