package com.gighub.auth.controller;

import java.util.Map;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gighub.auth.dto.AvailabilityResponse;
import com.gighub.auth.dto.SignupRequest;
import com.gighub.auth.dto.SignupResponse;
import com.gighub.auth.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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
}
