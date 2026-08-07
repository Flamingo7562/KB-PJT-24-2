package com.gighub.attendance.controller;

import com.gighub.attendance.dto.WorkplaceQrReissueResponse;
import com.gighub.attendance.dto.WorkplaceQrResponse;
import com.gighub.attendance.service.WorkplaceQrService;
import com.gighub.auth.security.AuthPrincipal;
import com.gighub.auth.security.AuthPrincipals;
import com.gighub.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 사업장 고정 QR 조회와 재발급을 제공하는 Controller입니다. */
@RestController
@RequestMapping("/api/workplaces/{workplaceId}/qr")
public class WorkplaceQrController {

    private final WorkplaceQrService workplaceQrService;

    public WorkplaceQrController(WorkplaceQrService workplaceQrService) {
        this.workplaceQrService = workplaceQrService;
    }

    /** 조회는 QR을 만들지도 교체하지도 않습니다. */
    @GetMapping
    public ResponseEntity<ApiResponse<WorkplaceQrResponse>> find(
            @PathVariable Long workplaceId,
            Authentication authentication) {
        AuthPrincipal principal = AuthPrincipals.resolve(authentication);

        return ResponseEntity.ok(
                ApiResponse.of(workplaceQrService.findQr(principal, workplaceId)));
    }

    /**
     * 기존 활성 QR을 즉시 폐기하고 새 QR을 발급합니다.
     *
     * <p>되돌릴 수 없습니다. 이미 인쇄해 매장에 부착한 QR이 이 시점부터 동작하지 않습니다.</p>
     */
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<WorkplaceQrReissueResponse>> reissue(
            @PathVariable Long workplaceId,
            Authentication authentication) {
        AuthPrincipal principal = AuthPrincipals.resolve(authentication);

        return ResponseEntity.ok(
                ApiResponse.of(workplaceQrService.reissue(principal, workplaceId)));
    }
}
