package com.gighub.auth.controller;

import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;

import com.gighub.auth.dto.AvailabilityResponse;
import com.gighub.auth.dto.EmailAvailabilityQuery;
import com.gighub.auth.dto.LoginIdAvailabilityQuery;
import com.gighub.auth.dto.SessionResponse;
import com.gighub.auth.dto.SignupRequest;
import com.gighub.auth.dto.SignupResponse;
import com.gighub.auth.security.AuthPrincipal;
import com.gighub.auth.service.AuthService;
import com.gighub.common.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 가입 전 CSRF 준비와 공개 Session 복원을 제공하는 인증 Controller입니다. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(HttpServletRequest request) {
        Object attribute = request.getAttribute(CsrfToken.class.getName());
        if (!(attribute instanceof CsrfToken)) {
            throw new IllegalStateException("Security Filter가 CSRF Token을 준비하지 않았습니다.");
        }

        // Spring Security 5.8의 지연 Token을 실제로 참조해야 XSRF-TOKEN Cookie가 기록됩니다.
        ((CsrfToken) attribute).getToken();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/session")
    public ResponseEntity<ApiResponse<SessionResponse>> session(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal)) {
            return ResponseEntity.ok(ApiResponse.of(SessionResponse.unauthenticated()));
        }

        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        SessionResponse response = SessionResponse.authenticated(
                principal.getRole(),
                principal.getName(),
                authService.needsWorkplaceSetup(principal)
        );
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/login-id-availability")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> loginIdAvailability(
            @Valid @ModelAttribute LoginIdAvailabilityQuery query) {
        AvailabilityResponse response = new AvailabilityResponse(
                authService.isLoginIdAvailable(query.getLoginId())
        );
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/email-availability")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> emailAvailability(
            @Valid @ModelAttribute EmailAvailabilityQuery query) {
        AvailabilityResponse response = new AvailabilityResponse(
                authService.isEmailAvailable(query.getEmail())
        );
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request) {
        SignupResponse response = new SignupResponse(authService.signup(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }
}
