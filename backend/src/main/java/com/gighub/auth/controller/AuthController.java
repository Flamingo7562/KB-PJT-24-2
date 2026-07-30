package com.gighub.auth.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gighub.auth.dto.AvailabilityResponse;
import com.gighub.auth.dto.LoginRequest;
import com.gighub.auth.dto.LoginResponse;
import com.gighub.auth.dto.SessionResponse;
import com.gighub.auth.dto.SignupRequest;
import com.gighub.auth.dto.SignupResponse;
import com.gighub.auth.security.AuthPrincipal;
import com.gighub.auth.security.SessionAuthenticator;
import com.gighub.auth.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionAuthenticator sessionAuthenticator;

    public AuthController(AuthService authService, SessionAuthenticator sessionAuthenticator) {
        this.authService = authService;
        this.sessionAuthenticator = sessionAuthenticator;
    }

    @GetMapping("/login-id-availability")
    public ResponseEntity<Map<String, Object>> checkLoginId(@RequestParam String loginId) {
        boolean available = authService.isLoginIdAvailable(loginId);
        return ResponseEntity.ok(Map.of("data", new AvailabilityResponse(available)));
    }

    @GetMapping("/email-availability")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        boolean available = authService.isEmailAvailable(email);
        return ResponseEntity.ok(Map.of("data", new AvailabilityResponse(available)));
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@Valid @RequestBody SignupRequest request) {
        Long userId = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", new SignupResponse(userId)));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
                                                       HttpServletRequest httpRequest,
                                                       HttpServletResponse httpResponse) {
        AuthPrincipal principal = authService.authenticate(request);
        sessionAuthenticator.login(httpRequest, httpResponse, principal);
        boolean needsWorkplaceSetup = authService.needsWorkplaceSetup(principal);
        LoginResponse response = new LoginResponse(principal.getRole(), principal.getName(), needsWorkplaceSetup);
        return ResponseEntity.ok(Map.of("data", response));
    }

    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> session() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal)) {
            return ResponseEntity.ok(Map.of("data", SessionResponse.unauthenticated()));
        }

        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        boolean needsWorkplaceSetup = authService.needsWorkplaceSetup(principal);
        SessionResponse response = SessionResponse.authenticated(principal.getRole(), principal.getName(),
                needsWorkplaceSetup);
        return ResponseEntity.ok(Map.of("data", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request, HttpServletResponse response) {
        sessionAuthenticator.logout(request, response);
        return ResponseEntity.ok(Map.of("data", Map.of()));
    }
}
