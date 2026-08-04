package com.gighub.badge.controller;

import com.gighub.badge.dto.UserBadge;
import com.gighub.badge.dto.UserBadgeListResponse;
import com.gighub.badge.mapper.BadgeQueryMapper;
import com.gighub.common.api.ApiResponse;
import com.gighub.common.exception.AuthRequiredException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class BadgeController {
    private static final String LOGIN_USER = "LOGIN_USER";

    private final BadgeQueryMapper badgeQueryMapper;

    @GetMapping("/api/users/me/badges")
    public ResponseEntity<ApiResponse<UserBadgeListResponse>> getMyBadge(HttpSession session){
        Long loginUserId = (Long) session.getAttribute(LOGIN_USER);
        if (loginUserId == null) {
            throw new AuthRequiredException("로그인이 필요합니다.");
        }

        List<UserBadge> badges = badgeQueryMapper.findBadgesByUserId(loginUserId);

        return ResponseEntity.ok(ApiResponse.of(UserBadgeListResponse.of(badges)));
    }
}
