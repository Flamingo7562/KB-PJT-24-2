package com.gighub.support;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.auth.security.AuthSessionManager;
import com.gighub.common.api.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로컬 수동 테스트 전용 가짜 로그인 (test-only).
 *
 * <p>정식 로그인과 같은 SecurityContext를 만들되 local 프로파일에서만 공개합니다.
 */
@RestController
@Profile("local")
public class TestLoginController {

    private final TestLoginService testLoginService;
    private final AuthSessionManager authSessionManager;

    public TestLoginController(
            TestLoginService testLoginService,
            AuthSessionManager authSessionManager) {
        this.testLoginService = testLoginService;
        this.authSessionManager = authSessionManager;
    }

    @GetMapping("/api/test-login/{userId}")
    public ResponseEntity<ApiResponse<TestLoginResponse>> login(
            @PathVariable Long userId,
            HttpServletRequest request,
            HttpServletResponse response) {
        AuthPrincipal principal = testLoginService.loadActivePrincipal(userId);
        authSessionManager.establish(request, response, principal);
        return ResponseEntity.ok(ApiResponse.of(new TestLoginResponse(userId)));
    }
}
