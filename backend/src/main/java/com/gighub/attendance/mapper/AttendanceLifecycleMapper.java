package com.gighub.attendance.mapper;

import com.gighub.attendance.mapper.result.AttendanceLifecycleWorkCaseRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 출퇴근 자동 상태 전이가 사용하는 후보 조회와 잠금 변경 진입점입니다. */
@Mapper
public interface AttendanceLifecycleMapper {

    /** READY 진입 시각이 된 ACCEPTED 근무를 찾습니다. */
    List<Long> findReadyCandidateIds(
            @Param("readyAt") LocalDateTime readyAt,
            @Param("noShowAfter") LocalDateTime noShowAfter);

    /** NO_SHOW 판정 시각이 된 READY 근무를 찾습니다. */
    List<Long> findNoShowCandidateIds(@Param("noShowAt") LocalDateTime noShowAt);

    /** 퇴근 누락 판정 시각이 된 IN_PROGRESS 근무를 찾습니다. */
    List<Long> findCheckoutMissingCandidateIds(
            @Param("checkoutMissingAt") LocalDateTime checkoutMissingAt);

    /** 상태 판단과 전이 사이에 스캔이 끼어들지 않도록 근무 행을 먼저 잠급니다. */
    AttendanceLifecycleWorkCaseRow lockById(@Param("workCaseId") long workCaseId);

    /** 수락·계약·예치·정산·현재 사업장 좌표가 모두 확정됐는지 확인합니다. */
    boolean isReadyAggregateComplete(@Param("workCaseId") long workCaseId);

    /** 성공한 출근 또는 퇴근 기록의 존재 여부를 확인합니다. */
    boolean hasSuccessfulAttendance(
            @Param("workCaseId") long workCaseId,
            @Param("attendanceType") String attendanceType);

    int transitionStatus(
            @Param("workCaseId") long workCaseId,
            @Param("expectedStatus") String expectedStatus,
            @Param("status") String status);
}
