package com.gighub.auth.mapper;

import org.apache.ibatis.annotations.Param;

/** OWNER의 현재 ACTIVE 사업장 수를 조회합니다. */
public interface WorkplaceCountMapper {

    int countActiveByOwnerUserId(@Param("ownerUserId") Long ownerUserId);
}
