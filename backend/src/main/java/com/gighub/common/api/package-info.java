/**
 * 클라이언트에 공개하는 공통 성공·오류 응답 계약을 정의합니다.
 *
 * <p>오류는 {@link ApiErrorCode}가 보호 명세에서 승인한 Code만 제공하고,
 * {@link ApiErrorResponse}가 {@code code/message/traceId/fieldErrors?} 구조를 표현합니다.
 * 입력 필드 오류가 없으면 선택 항목인 {@code fieldErrors}는 JSON에서 생략합니다.</p>
 *
 * <p>성공은 {@link ApiResponse}가 단일 결과를 {@code {data}}로 감싸고, 목록은
 * {@link PageResponse}와 {@link PageMeta}가 {@code {data:{content,page}}}를 표현합니다.
 * 목록 Query의 기본값과 상한은 {@link PageRequests} 하나로 관리합니다.</p>
 *
 * <p>응답 모델은 생성 이후 값이 바뀌지 않는 읽기 전용 객체로 사용합니다. 새로운 오류
 * Code나 응답 필드는 구현 편의를 위해 임의로 추가하지 않고, 명세 승인을 먼저 반영합니다.
 * {@code 204 No Content}, 파일 Stream과 정적 자원은 성공 Envelope로 감싸지 않습니다.</p>
 */
package com.gighub.common.api;

