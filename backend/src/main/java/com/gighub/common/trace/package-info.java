/**
 * 한 HTTP 요청의 오류 응답과 서버 로그를 연결하는 추적 식별자를 관리합니다.
 *
 * <p>{@link TraceIds}는 요청 Attribute에 UUID를 한 번 생성해 같은 요청 안에서
 * 재사용합니다. {@link TraceIdFilter}는 이 값을 SLF4J MDC에 넣어 처리 중인 로그가
 * 동일한 {@code traceId}를 사용하도록 합니다.</p>
 *
 * <p>요청 처리가 끝나면 Tomcat Thread Pool의 다음 요청으로 값이 넘어가지 않도록 MDC를
 * 반드시 정리합니다. Filter가 없는 독립형 MVC 테스트에서는 {@code TraceIds}가 식별자를
 * 보완 생성합니다.</p>
 */
package com.gighub.common.trace;
