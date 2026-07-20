package com.gighub.health.controller;

import java.time.Instant;

import com.gighub.health.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tomcat과 Spring MVC의 정상 기동 여부를 확인하는 HTTP Endpoint입니다.
 *
 * <p>현재 Endpoint는 외부 시스템이나 DB를 조회하지 않으므로 단순한 liveness 확인에만 사용합니다.
 * 향후 Spring Security를 적용해도 이 경로는 공개 상태로 유지할지 별도로 검토해야 합니다.</p>
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    /**
     * 현재 백엔드 프로세스가 HTTP 요청을 처리할 수 있음을 반환합니다.
     *
     * @return 서비스 이름, 상태와 서버 확인 시각
     */
    @GetMapping
    public HealthResponse health() {
        // TODO: API 공통 응답 규격이 확정되면 동일한 Envelope를 적용합니다.
        return new HealthResponse("gig-hub-backend", "UP", Instant.now());
    }
}

