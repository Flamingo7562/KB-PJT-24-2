/**
 * 클라이언트에 공개하는 공통 오류 응답 계약을 정의합니다.
 *
 * <p>{@link ApiErrorCode}는 보호 명세에서 승인한 오류 Code만 제공하고,
 * {@link ApiErrorResponse}는 {@code code/message/traceId/fieldErrors?} 구조를 표현합니다.
 * 입력 필드 오류가 없으면 선택 항목인 {@code fieldErrors}는 JSON에서 생략합니다.</p>
 *
 * <p>응답 모델은 생성 이후 값이 바뀌지 않는 읽기 전용 객체로 사용합니다. 새로운 오류
 * Code나 응답 필드는 구현 편의를 위해 임의로 추가하지 않고, 명세 승인을 먼저 반영합니다.
 * 성공·목록 Envelope는 별도 구현 범위가 확정될 때 이 패키지에 추가합니다.</p>
 */
package com.gighub.common.api;

