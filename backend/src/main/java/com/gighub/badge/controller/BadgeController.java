package com.gighub.badge.controller;

import com.gighub.auth.security.AuthPrincipals;
import com.gighub.badge.dto.UserBadge;
import com.gighub.badge.dto.UserBadgeListResponse;
import com.gighub.badge.mapper.BadgeQueryMapper;
import com.gighub.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeQueryMapper badgeQueryMapper;

    @GetMapping("/api/users/me/badges")
    public ResponseEntity<ApiResponse<UserBadgeListResponse>> getMyBadge(
            Authentication authentication) {
        Long loginUserId = AuthPrincipals.resolve(authentication).getUserId();

        List<UserBadge> badges = badgeQueryMapper.findBadgesByUserId(loginUserId);

        return ResponseEntity.ok(ApiResponse.of(UserBadgeListResponse.of(badges)));
    }
}
