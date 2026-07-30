package com.gighub.work.mapper;

import com.gighub.work.dto.WorkCaseEscrowContext;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface WorkMapper {
    WorkCaseEscrowContext getEscrowContextForUpdate(
            @Param("workCaseId") Long workCaseId);

    // 근무 건의 상태(status)를 업데이트
    int updateWorkStatus(@Param("workCaseId") Long workCaseId,
                         @Param("fromStatuses") List<String> fromStatuses,
                         @Param("toStatus") String toStatus);

    Long getWorkerIdByWorkCaseId(@Param("workCaseId") Long workCaseId);

    Long getAgreedWageByWorkCaseId(@Param("workCaseId") Long workCaseId);
}
