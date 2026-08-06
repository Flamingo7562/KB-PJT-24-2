package com.gighub.member.controller;

import javax.validation.Valid;

import com.gighub.auth.security.AuthPrincipals;
import com.gighub.common.api.ApiResponse;
import com.gighub.member.dto.UserProfileResponse;
import com.gighub.member.dto.UserProfileUpdateRequest;
import com.gighub.member.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 내 프로필 조회. 요청에서 사용자 ID를 받지 않고 인증 Principal만 사용한다.
    @GetMapping("/api/users/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            Authentication authentication) {
        Long userId = AuthPrincipals.resolve(authentication).getUserId();

        return ResponseEntity.ok(ApiResponse.of(userService.getProfile(userId)));
    }

    // 내 전화번호 수정. 승인 명세에 따라 phone 외 필드는 요청 DTO에서 거부된다.
    @PatchMapping("/api/users/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyPhone(
            Authentication authentication,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        Long userId = AuthPrincipals.resolve(authentication).getUserId();

        return ResponseEntity.ok(
                ApiResponse.of(userService.updatePhone(userId, request.getPhone())));
    }
}