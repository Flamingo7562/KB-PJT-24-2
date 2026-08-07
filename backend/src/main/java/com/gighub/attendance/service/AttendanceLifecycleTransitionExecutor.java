package com.gighub.attendance.service;

import com.gighub.attendance.mapper.AttendanceLifecycleMapper;
import com.gighub.attendance.mapper.result.AttendanceLifecycleWorkCaseRow;
import com.gighub.work.domain.WorkCaseStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 후보 근무 하나를 잠근 뒤 자동 상태 전이 조건을 다시 확인합니다. */
@Service
public class AttendanceLifecycleTransitionExecutor {

    private static final String CHECK_IN = "CHECK_IN";
    private static final String CHECK_OUT = "CHECK_OUT";

    private final AttendanceLifecycleMapper lifecycleMapper;
    private final SignedContractArtifactVerifier artifactVerifier;

    public AttendanceLifecycleTransitionExecutor(
            AttendanceLifecycleMapper lifecycleMapper,
            SignedContractArtifactVerifier artifactVerifier) {
        this.lifecycleMapper = lifecycleMapper;
        this.artifactVerifier = artifactVerifier;
    }

    @Transactional
    public boolean advanceToReady(long workCaseId, LocalDateTime now) {
        AttendanceLifecycleWorkCaseRow row = lifecycleMapper.lockById(workCaseId);
        if (row == null
                || row.getStatus() != WorkCaseStatus.ACCEPTED
                || row.getStartsAt().isAfter(now.plusMinutes(30))
                || !now.isBefore(row.getStartsAt().plusHours(1))) {
            return false;
        }
        if (!lifecycleMapper.isReadyAggregateComplete(workCaseId)
                || !artifactVerifier.isReadable(workCaseId)) {
            return false;
        }
        return lifecycleMapper.transitionStatus(
                workCaseId, WorkCaseStatus.ACCEPTED.name(), WorkCaseStatus.READY.name()) == 1;
    }

    @Transactional
    public boolean advanceToNoShow(long workCaseId, LocalDateTime now) {
        AttendanceLifecycleWorkCaseRow row = lifecycleMapper.lockById(workCaseId);
        if (row == null
                || row.getStatus() != WorkCaseStatus.READY
                || row.getStartsAt().plusHours(1).isAfter(now)
                || lifecycleMapper.hasSuccessfulAttendance(workCaseId, CHECK_IN)) {
            return false;
        }
        return lifecycleMapper.transitionStatus(
                workCaseId, WorkCaseStatus.READY.name(), WorkCaseStatus.NO_SHOW.name()) == 1;
    }

    @Transactional
    public boolean advanceToCheckoutMissing(long workCaseId, LocalDateTime now) {
        AttendanceLifecycleWorkCaseRow row = lifecycleMapper.lockById(workCaseId);
        if (row == null
                || row.getStatus() != WorkCaseStatus.IN_PROGRESS
                || row.getEndsAt().plusHours(2).isAfter(now)
                || !lifecycleMapper.hasSuccessfulAttendance(workCaseId, CHECK_IN)
                || lifecycleMapper.hasSuccessfulAttendance(workCaseId, CHECK_OUT)) {
            return false;
        }
        return lifecycleMapper.transitionStatus(
                workCaseId,
                WorkCaseStatus.IN_PROGRESS.name(),
                WorkCaseStatus.CHECK_OUT_MISSING.name()) == 1;
    }
}
