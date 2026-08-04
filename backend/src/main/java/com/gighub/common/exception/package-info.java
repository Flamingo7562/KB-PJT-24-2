/**
 * 애플리케이션 예외를 승인된 공통 오류 응답으로 변환합니다.
 *
 * <p>도메인 예외는 {@link ApiException}을 통해 HTTP 상태, 승인 오류 Code와 외부에
 * 노출해도 안전한 메시지를 전달합니다. 전역 처리기는 구체적인 지갑·은행·문서 예외를
 * 알지 않고 이 공통 계약만 처리합니다.</p>
 *
 * <h2>처리 원칙</h2>
 * <ul>
 *   <li>입력 검증과 잘못된 요청 형식은 {@code VALIDATION_ERROR}로 정규화합니다.</li>
 *   <li>예상 가능한 도메인 오류는 {@code ApiException}이 지정한 상태와 Code를 사용합니다.</li>
 *   <li>예상하지 못한 오류는 내부 원인을 로그에 남기고 안전한 {@code INTERNAL_ERROR}로
 *   응답합니다.</li>
 *   <li>내부 예외 메시지, SQL과 Stack Trace는 클라이언트 응답에 포함하지 않습니다.</li>
 * </ul>
 *
 * <p>오류 응답에는 요청의 추적 식별자를 포함해 같은 식별자의 서버 로그를 찾을 수 있게
 * 합니다.</p>
 */
package com.gighub.common.exception;

