package com.gighub.work.service.impl;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.api.PageRequests;
import com.gighub.common.api.PageResponse;
import com.gighub.common.exception.RoleMismatchException;
import com.gighub.member.domain.UserRole;
import com.gighub.work.dto.WorkerHomeResponse;
import com.gighub.work.dto.WorkerWorkCaseResponse;
import com.gighub.work.mapper.WorkerWorkCaseMapper;
import com.gighub.work.mapper.result.WorkerWorkCaseRow;
import com.gighub.work.service.WorkerWorkCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WorkerWorkCaseServiceImpl implements WorkerWorkCaseService {

    private static final ZoneId DATABASE_ZONE = ZoneId.of("Asia/Seoul");

    private final WorkerWorkCaseMapper workerWorkCaseMapper;
    private final Clock clock;

    @Autowired
    public WorkerWorkCaseServiceImpl(WorkerWorkCaseMapper workerWorkCaseMapper) {
        this(workerWorkCaseMapper, Clock.system(DATABASE_ZONE));
    }

    /** 오늘 경계 테스트에서만 고정 Clock을 주입합니다. */
    WorkerWorkCaseServiceImpl(WorkerWorkCaseMapper workerWorkCaseMapper, Clock clock) {
        this.workerWorkCaseMapper = workerWorkCaseMapper;
        this.clock = clock;
    }

    @Override
    public WorkerHomeResponse home(AuthPrincipal principal) {
        requireWorker(principal);
        LocalDate today = LocalDate.now(clock);
        LocalDateTime todayStart = today.atStartOfDay();
        WorkerWorkCaseRow row = workerWorkCaseMapper.findToday(
                principal.getUserId(),
                todayStart.minusDays(1),
                todayStart,
                todayStart.plusDays(1));
        return new WorkerHomeResponse(row == null ? null : WorkerWorkCaseResponse.from(row));
    }

    @Override
    public PageResponse<WorkerWorkCaseResponse> list(
            AuthPrincipal principal,
            int page,
            int size) {
        requireWorker(principal);
        PageRequests.validate(page, size);
        List<WorkerWorkCaseResponse> content = workerWorkCaseMapper.findPage(
                        principal.getUserId(), size, PageRequests.offset(page, size))
                .stream()
                .map(WorkerWorkCaseResponse::from)
                .collect(Collectors.toList());
        return PageResponse.of(content, page, size, workerWorkCaseMapper.count(principal.getUserId()));
    }

    private void requireWorker(AuthPrincipal principal) {
        if (principal == null || principal.getRole() != UserRole.WORKER) {
            throw new RoleMismatchException("WORKER만 근무 내역을 조회할 수 있습니다.");
        }
    }
}
