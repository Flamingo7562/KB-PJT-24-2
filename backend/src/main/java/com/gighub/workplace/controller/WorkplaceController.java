package com.gighub.workplace.controller;

import javax.validation.Valid;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.auth.security.AuthPrincipals;
import com.gighub.common.api.ApiResponse;
import com.gighub.workplace.dto.WorkplaceCreateRequest;
import com.gighub.workplace.dto.WorkplaceCreateResponse;
import com.gighub.workplace.service.WorkplaceService;
import com.gighub.workplace.service.command.WorkplaceCreateCommand;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 사업장 등록과 소유 사업장 조회를 제공하는 Controller입니다. */
@RestController
@RequestMapping("/api/workplaces")
public class WorkplaceController {

    private final WorkplaceService workplaceService;

    public WorkplaceController(WorkplaceService workplaceService) {
        this.workplaceService = workplaceService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkplaceCreateResponse>> create(
            @Valid @RequestBody WorkplaceCreateRequest request,
            Authentication authentication) {
        AuthPrincipal principal = AuthPrincipals.resolve(authentication);
        Long workplaceId = workplaceService.create(principal, toCommand(request));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(new WorkplaceCreateResponse(workplaceId)));
    }

    /**
     * 검증을 통과한 요청을 Service 입력으로 옮깁니다.
     *
     * <p>요청 DTO를 그대로 넘기지 않아 Service가 HTTP·JSON 계약에 의존하지 않습니다.
     * 소유자는 여기서 옮기지 않고 Service가 인증 Principal에서 직접 채웁니다.</p>
     */
    private WorkplaceCreateCommand toCommand(WorkplaceCreateRequest request) {
        return WorkplaceCreateCommand.builder()
                .businessRegistrationNumber(request.getBusinessRegistrationNumber())
                .name(request.getName())
                .representativeName(request.getRepresentativeName())
                .roadAddress(request.getRoadAddress())
                .detailAddress(request.getDetailAddress())
                .phone(request.getPhone())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
    }
}
