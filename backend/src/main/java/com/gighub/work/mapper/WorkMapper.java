package com.gighub.work.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface WorkMapper {
    // 근무 건의 상태(status)를 업데이트
    int updateWorkStatus(@Param("workCaseId") Long workCaseId, @Param("status") String status);

    Long getWorkerIdByWorkCaseId(Long workCaseId);
    BigDecimal getAgreedWageByWorkCaseId(Long workCaseId);
}
