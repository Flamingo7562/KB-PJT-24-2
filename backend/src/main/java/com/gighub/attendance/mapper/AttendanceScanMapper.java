package com.gighub.attendance.mapper;

import com.gighub.attendance.mapper.param.AttendanceRecordInsertParam;
import com.gighub.attendance.mapper.result.AttendanceCandidateRow;
import com.gighub.attendance.mapper.result.AttendanceScanWorkCaseRow;
import com.gighub.attendance.mapper.result.AttendanceWorkplaceRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** QR 스캔의 사업장·근무 잠금, 후보 조회와 근태 기록 진입점입니다. */
@Mapper
public interface AttendanceScanMapper {

    AttendanceWorkplaceRow findWorkplace(@Param("workplaceId") long workplaceId);

    AttendanceWorkplaceRow lockWorkplace(@Param("workplaceId") long workplaceId);

    List<AttendanceCandidateRow> findCandidates(
            @Param("workerId") long workerId,
            @Param("workplaceId") long workplaceId,
            @Param("readyAt") LocalDateTime readyAt,
            @Param("noShowAfter") LocalDateTime noShowAfter,
            @Param("checkoutAfter") LocalDateTime checkoutAfter);

    List<Long> findCompletedCandidateIds(
            @Param("workerId") long workerId,
            @Param("workplaceId") long workplaceId,
            @Param("readyAt") LocalDateTime readyAt,
            @Param("checkoutAfter") LocalDateTime checkoutAfter);

    AttendanceScanWorkCaseRow lockWorkCase(@Param("workCaseId") long workCaseId);

    boolean hasSuccessfulAttendance(
            @Param("workCaseId") long workCaseId,
            @Param("attendanceType") String attendanceType);

    int insertRecord(AttendanceRecordInsertParam param);

    int transitionStatus(
            @Param("workCaseId") long workCaseId,
            @Param("expectedStatus") String expectedStatus,
            @Param("status") String status);
}
