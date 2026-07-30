package com.gighub.badge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBadge {
    private Long id;
    private Long userId;
    private String badgeType;
    private String evidence;
    private LocalDateTime createdAt;
}
