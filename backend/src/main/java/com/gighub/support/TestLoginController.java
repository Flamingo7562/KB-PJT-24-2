package com.gighub.support;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * 로컬 수동 테스트 전용 가짜 로그인 (test-only).
 *
 * <p>인증 파트가 아직 구현되지 않아 에스크로 예치·정산 흐름을 확인하기 위해서만 존재한다.
 * 세션 키는 {@code LOGIN_USER}, 값은 {@code Long} userId로 가정한다.
 *
 * <p>TODO(후속): 정식 로그인 도입 시 이 클래스를 제거하고 세션 규약을 인증 파트와 통일한다.
 */
@RestController
public class TestLoginController {

    @GetMapping("/api/test-login/{userId}")
    public ResponseEntity<Map<String, Object>> login(@PathVariable Long userId,
                                                     HttpSession session) {
        session.setAttribute("LOGIN_USER", userId);
        return ResponseEntity.ok(Map.of("data", Map.of("loginUserId", userId)));
    }
}