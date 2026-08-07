package com.gighub.attendance.service;

import com.gighub.attendance.mapper.AttendanceLifecycleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.BiFunction;

/** READY·NO_SHOW·CHECK_OUT_MISSING를 같은 서버 자동 작업에서 처리합니다. */
@Component
public class AttendanceLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(AttendanceLifecycleScheduler.class);
    private static final ZoneId DATABASE_ZONE = ZoneId.of("Asia/Seoul");

    private final AttendanceLifecycleMapper lifecycleMapper;
    private final AttendanceLifecycleTransitionExecutor transitionExecutor;
    private final Clock clock;

    @Autowired
    public AttendanceLifecycleScheduler(
            AttendanceLifecycleMapper lifecycleMapper,
            AttendanceLifecycleTransitionExecutor transitionExecutor) {
        this(lifecycleMapper, transitionExecutor, Clock.system(DATABASE_ZONE));
    }

    /** 경계 시각 테스트에서만 고정 Clock을 주입합니다. */
    AttendanceLifecycleScheduler(
            AttendanceLifecycleMapper lifecycleMapper,
            AttendanceLifecycleTransitionExecutor transitionExecutor,
            Clock clock) {
        this.lifecycleMapper = lifecycleMapper;
        this.transitionExecutor = transitionExecutor;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 60_000L, initialDelay = 60_000L)
    public void runOnce() {
        LocalDateTime now = LocalDateTime.now(clock);

        // 같은 기준 시각으로 READY를 먼저 만든 뒤, 장기 중단으로 지난 NO_SHOW도 이번 주기에 따라잡습니다.
        process(
                "READY",
                lifecycleMapper.findReadyCandidateIds(now.plusMinutes(30), now.minusHours(1)),
                now,
                transitionExecutor::advanceToReady);
        process(
                "NO_SHOW",
                lifecycleMapper.findNoShowCandidateIds(now.minusHours(1)),
                now,
                transitionExecutor::advanceToNoShow);
        process(
                "CHECK_OUT_MISSING",
                lifecycleMapper.findCheckoutMissingCandidateIds(now.minusHours(2)),
                now,
                transitionExecutor::advanceToCheckoutMissing);
    }

    private void process(
            String targetStatus,
            List<Long> workCaseIds,
            LocalDateTime now,
            BiFunction<Long, LocalDateTime, Boolean> transition) {
        int changed = 0;
        for (Long workCaseId : workCaseIds) {
            try {
                if (transition.apply(workCaseId, now)) {
                    changed++;
                }
            } catch (RuntimeException failure) {
                // 한 근무의 저장소·DB 오류가 다른 근무의 자동 전이까지 막지 않게 격리합니다.
                log.warn(
                        "출퇴근 자동 상태 전이에 실패했습니다. targetStatus={}, workCaseId={}",
                        targetStatus,
                        workCaseId,
                        failure);
            }
        }
        if (changed > 0) {
            log.info("출퇴근 자동 상태 전이를 완료했습니다. targetStatus={}, count={}", targetStatus, changed);
        }
    }
}
