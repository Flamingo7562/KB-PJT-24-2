package com.gighub.badge.mapper;

import com.gighub.badge.dto.UserBadge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BadgeQueryMapper {
    List<UserBadge> findBadgesByUserId(@Param("userId") Long userId);
}
