package com.gighub.auth.service.impl;

import com.gighub.auth.mapper.WorkplaceCountMapper;
import com.gighub.auth.security.AuthPrincipal;
import com.gighub.auth.service.AuthService;
import com.gighub.member.domain.UserRole;
import org.springframework.stereotype.Service;

/** 승인된 인증 계약을 DB 현재 상태로 계산합니다. */
@Service
public class AuthServiceImpl implements AuthService {

    private final WorkplaceCountMapper workplaceCountMapper;

    public AuthServiceImpl(WorkplaceCountMapper workplaceCountMapper) {
        this.workplaceCountMapper = workplaceCountMapper;
    }

    @Override
    public boolean needsWorkplaceSetup(AuthPrincipal principal) {
        if (principal.getRole() != UserRole.OWNER) {
            return false;
        }
        // 사업장 생성·비활성화가 즉시 반영되도록 Session에 계산 결과를 저장하지 않습니다.
        return workplaceCountMapper.countActiveByOwnerUserId(principal.getUserId()) == 0;
    }
}
