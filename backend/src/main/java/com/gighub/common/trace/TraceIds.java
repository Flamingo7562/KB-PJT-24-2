package com.gighub.common.trace;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

/** 요청 범위의 추적 식별자를 생성하고 공유합니다. */
public final class TraceIds {

    public static final String REQUEST_ATTRIBUTE = TraceIds.class.getName() + ".traceId";

    private TraceIds() {
    }

    /**
     * Filter가 만든 식별자를 재사용하고, 독립형 MVC 테스트처럼 Filter가 없을 때만 보완합니다.
     *
     * @param request 현재 HTTP 요청
     * @return 요청 안에서 계속 재사용할 UUID 문자열
     */
    public static String getOrCreate(HttpServletRequest request) {
        Object current = request.getAttribute(REQUEST_ATTRIBUTE);
        if (current instanceof String && !((String) current).isBlank()) {
            return (String) current;
        }

        String traceId = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ATTRIBUTE, traceId);
        return traceId;
    }
}
