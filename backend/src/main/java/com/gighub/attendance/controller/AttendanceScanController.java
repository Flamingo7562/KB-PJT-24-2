package com.gighub.attendance.controller;

import com.gighub.attendance.dto.AttendanceScanRequest;
import com.gighub.attendance.dto.AttendanceScanResponse;
import com.gighub.attendance.service.AttendanceScanResult;
import com.gighub.attendance.service.AttendanceScanService;
import com.gighub.auth.security.AuthPrincipal;
import com.gighub.auth.security.AuthPrincipals;
import com.gighub.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/** WORKER 출퇴근 QR 스캔과 멱등 Replay를 제공합니다. */
@RestController
@RequestMapping("/api/attendance/scans")
public class AttendanceScanController {

    private static final String REPLAYED_HEADER = "Idempotency-Replayed";

    private final AttendanceScanService scanService;

    public AttendanceScanController(AttendanceScanService scanService) {
        this.scanService = scanService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AttendanceScanResponse>> scan(
            @Valid @RequestBody AttendanceScanRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication) {
        AuthPrincipal principal = AuthPrincipals.resolve(authentication);
        AttendanceScanResult result = scanService.scan(principal, request, idempotencyKey);

        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (result.isReplayed()) {
            response.header(REPLAYED_HEADER, "true");
        }
        return response.body(ApiResponse.of(result.getResponse()));
    }
}
