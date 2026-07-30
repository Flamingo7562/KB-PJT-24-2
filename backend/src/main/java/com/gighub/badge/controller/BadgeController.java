package com.gighub.badge.controller;

import com.gighub.badge.dto.UserBadge;
import com.gighub.badge.mapper.BadgeQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class BadgeController {
    private static final String LOGIN_USER = "LOGIN_USER";

    private final BadgeQueryMapper badgeQueryMapper;

    @GetMapping("/api/users/me/badges")
    public ResponseEntity<Map<String, Object>> getMyBadge(HttpSession session){
        Long loginUserId = (Long) session.getAttribute(LOGIN_USER);
//        if (loginUserId == null) {
//            throw new AuthRequiredException("로그인이 필요합니다.");
//        }

        List<UserBadge> badges = badgeQueryMapper.findBadgesByUserId(loginUserId);

        return ResponseEntity.ok(Map.of("data", Map.of("items", badges)));
    }
}
