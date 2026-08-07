package com.gighub.attendance.service.impl;

import com.gighub.attendance.dto.WorkplaceQrResponse;
import com.gighub.attendance.exception.WorkplaceQrIntegrityException;
import com.gighub.attendance.mapper.QrTokenMapper;
import com.gighub.attendance.mapper.result.QrTokenRow;
import com.gighub.attendance.qr.QrTokenCodec;
import com.gighub.attendance.service.WorkplaceQrService;
import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.api.ApiTimes;
import com.gighub.common.exception.ResourceNotFoundException;
import com.gighub.common.exception.RoleMismatchException;
import com.gighub.member.domain.UserRole;
import com.gighub.workplace.mapper.WorkplaceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사업장 고정 QR 계약을 인증 Principal과 DB 현재 상태로 적용합니다. */
@Service
public class WorkplaceQrServiceImpl implements WorkplaceQrService {

    private final WorkplaceMapper workplaceMapper;
    private final QrTokenMapper qrTokenMapper;
    private final QrTokenCodec qrTokenCodec;

    public WorkplaceQrServiceImpl(
            WorkplaceMapper workplaceMapper,
            QrTokenMapper qrTokenMapper,
            QrTokenCodec qrTokenCodec) {
        this.workplaceMapper = workplaceMapper;
        this.qrTokenMapper = qrTokenMapper;
        this.qrTokenCodec = qrTokenCodec;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkplaceQrResponse findQr(AuthPrincipal principal, Long workplaceId) {
        requireOwnedWorkplace(principal, workplaceId);

        QrTokenRow row = qrTokenMapper.findActiveByWorkplaceId(workplaceId);
        if (row == null) {
            // 조회는 QR을 만들지 않습니다. 여기서 조용히 발급하면 읽기 요청이 쓰기가 되고,
            // 애초에 발급이 누락된 사실도 드러나지 않습니다.
            throw new WorkplaceQrIntegrityException(
                    "ACTIVE 사업장 " + workplaceId + "에 활성 고정 QR이 없습니다.");
        }

        return new WorkplaceQrResponse(
                workplaceId,
                qrTokenCodec.sign(workplaceId, row.getTokenNonce()),
                ApiTimes.toInstant(row.getCreatedAt()));
    }

    /**
     * 역할을 먼저 확인합니다.
     *
     * <p>역할이 다른 호출자에게 소유권 판단 결과를 돌려주면 사업장 식별자의 존재 여부를
     * 알려주게 됩니다.</p>
     */
    private void requireOwnerRole(AuthPrincipal principal) {
        if (principal.getRole() != UserRole.OWNER) {
            throw new RoleMismatchException("사업장 QR은 OWNER만 사용할 수 있습니다.");
        }
    }

    /**
     * 역할과 소유권을 확인합니다.
     *
     * <p>없는 사업장과 남의 사업장을 구분하지 않습니다. 구분하면 비소유자가 식별자의 존재를
     * 알아낼 수 있습니다.</p>
     */
    private void requireOwnedWorkplace(AuthPrincipal principal, Long workplaceId) {
        requireOwnerRole(principal);
        if (workplaceMapper.countOwnedActiveById(workplaceId, principal.getUserId()) != 1) {
            throw new ResourceNotFoundException("사업장을 찾을 수 없습니다.");
        }
    }
}
