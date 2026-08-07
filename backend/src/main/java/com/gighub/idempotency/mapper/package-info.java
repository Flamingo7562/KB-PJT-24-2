/**
 * 멱등 요청 Claim의 저장과 상태 전이 SQL을 담당합니다.
 *
 * <p>Key 원문은 저장하지만 Fingerprint의 입력값(PIN·Token 원문 등)은 저장하지 않습니다.</p>
 */
package com.gighub.idempotency.mapper;
