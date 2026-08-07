package com.gighub.work.controller;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.auth.security.AuthPrincipals;
import com.gighub.common.api.ApiResponse;
import com.gighub.common.api.PageRequests;
import com.gighub.common.api.PageResponse;
import com.gighub.work.dto.WorkerHomeResponse;
import com.gighub.work.dto.WorkerWorkCaseResponse;
import com.gighub.work.service.WorkerWorkCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WorkerWorkCaseController {

    private final WorkerWorkCaseService workerWorkCaseService;

    @GetMapping("/api/worker/home")
    public ResponseEntity<ApiResponse<WorkerHomeResponse>> home(Authentication authentication) {
        AuthPrincipal principal = AuthPrincipals.resolve(authentication);
        return ResponseEntity.ok(ApiResponse.of(workerWorkCaseService.home(principal)));
    }

    @GetMapping("/api/worker/work-cases")
    public ResponseEntity<ApiResponse<PageResponse<WorkerWorkCaseResponse>>> list(
            @RequestParam(defaultValue = PageRequests.DEFAULT_PAGE_TEXT) int page,
            @RequestParam(defaultValue = PageRequests.DEFAULT_SIZE_TEXT) int size,
            Authentication authentication) {
        AuthPrincipal principal = AuthPrincipals.resolve(authentication);
        return ResponseEntity.ok(ApiResponse.of(workerWorkCaseService.list(principal, page, size)));
    }
}
