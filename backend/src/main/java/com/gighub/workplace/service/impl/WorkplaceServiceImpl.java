package com.gighub.workplace.service.impl;

import java.util.List;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.api.PageRequests;
import com.gighub.common.api.PageResponse;
import com.gighub.common.exception.ForbiddenException;
import com.gighub.common.exception.ConflictException;
import com.gighub.member.domain.UserRole;
import com.gighub.workplace.dto.WorkplaceListItemResponse;
import com.gighub.workplace.mapper.WorkplaceMapper;
import com.gighub.workplace.mapper.param.WorkplaceInsertParam;
import com.gighub.workplace.mapper.result.WorkplaceListRow;
import com.gighub.workplace.service.WorkplaceService;
import com.gighub.workplace.service.command.WorkplaceCreateCommand;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 승인된 사업장 계약을 인증 Principal과 DB 현재 상태로 적용합니다. */
@Service
public class WorkplaceServiceImpl implements WorkplaceService {

    private final WorkplaceMapper workplaceMapper;

    public WorkplaceServiceImpl(WorkplaceMapper workplaceMapper) {
        this.workplaceMapper = workplaceMapper;
    }

    @Override
    @Transactional
    public Long create(AuthPrincipal principal, WorkplaceCreateCommand command) {
        requireOwner(principal, "사업장은 OWNER만 등록할 수 있습니다.");

        WorkplaceInsertParam param = WorkplaceInsertParam.builder()
                // 소유자는 요청 Body가 아니라 인증 Principal에서만 정합니다.
                .ownerUserId(principal.getUserId())
                .businessRegistrationNumber(command.getBusinessRegistrationNumber())
                .name(command.getName())
                .representativeName(command.getRepresentativeName())
                .roadAddress(command.getRoadAddress())
                .detailAddress(command.getDetailAddress())
                .phone(command.getPhone())
                .latitude(command.getLatitude())
                .longitude(command.getLongitude())
                .build();

        insertOrReportDuplicate(param);
        return param.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WorkplaceListItemResponse> findOwnedWorkplaces(
            AuthPrincipal principal,
            int page,
            int size) {
        requireOwner(principal, "사업장 목록은 OWNER만 조회할 수 있습니다.");
        // 역할을 먼저 확인합니다. Page 값이 잘못된 요청이라도 권한 없는 호출자에게 400을
        // 돌려주면 Endpoint의 존재와 Query 규칙을 알려주게 됩니다.
        PageRequests.validate(page, size);

        Long ownerUserId = principal.getUserId();
        long totalElements = workplaceMapper.countByOwnerUserId(ownerUserId);
        List<WorkplaceListRow> rows = workplaceMapper.findPageByOwnerUserId(
                ownerUserId, size, PageRequests.offset(page, size));

        List<WorkplaceListItemResponse> content = rows.stream()
                .map(WorkplaceListItemResponse::from)
                .toList();

        return PageResponse.of(content, page, size, totalElements);
    }

    /**
     * Security 설정은 인증 여부만 강제하므로 역할 경계는 도메인에서 확인합니다.
     *
     * <p>OWNER가 아닌 모든 인증 사용자를 거절합니다. 허용 역할을 지정하지 않고 OWNER만
     * 통과시키므로 나중에 역할이 늘어도 기본값이 거절로 유지됩니다.</p>
     */
    private void requireOwner(AuthPrincipal principal, String message) {
        if (principal.getRole() != UserRole.OWNER) {
            throw new ForbiddenException(message);
        }
    }

    /**
     * 사업자등록번호 중복을 Unique 제약으로 판정합니다.
     *
     * <p>사전 조회 후 저장하면 두 요청이 같은 시점에 "없음"을 확인하고 모두 저장할 수
     * 있습니다. 조회와 저장 사이를 막을 수 있는 것은 DB 제약뿐이라 예외를 승인된 충돌
     * 응답으로 바꿉니다.</p>
     */
    private void insertOrReportDuplicate(WorkplaceInsertParam param) {
        try {
            workplaceMapper.insert(param);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("이미 등록된 사업자등록번호입니다.");
        }
    }
}
