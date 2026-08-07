package com.gighub.attendance.service;

import com.gighub.attendance.dto.AttendanceScanRequest;
import com.gighub.auth.security.AuthPrincipal;

/** 멱등 Claim을 포함한 출퇴근 QR 스캔 진입점입니다. */
public interface AttendanceScanService {

    AttendanceScanResult scan(
            AuthPrincipal principal,
            AttendanceScanRequest request,
            String idempotencyKey);
}
