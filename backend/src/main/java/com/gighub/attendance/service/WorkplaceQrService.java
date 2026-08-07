package com.gighub.attendance.service;

import com.gighub.attendance.dto.WorkplaceQrResponse;
import com.gighub.auth.security.AuthPrincipal;

/** 사업장 고정 QR 조회와 재발급 진입점입니다. */
public interface WorkplaceQrService {

    /**
     * 해당 사업장 OWNER에게 현재 활성 고정 QR을 돌려줍니다.
     *
     * <p>QR을 만들지도 교체하지도 않습니다. 같은 사업장을 반복 조회하면 저장된 nonce로부터
     * 매번 같은 Token이 나옵니다.</p>
     */
    WorkplaceQrResponse findQr(AuthPrincipal principal, Long workplaceId);
}
