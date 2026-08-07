package com.gighub.work.service;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.api.PageResponse;
import com.gighub.work.dto.WorkerHomeResponse;
import com.gighub.work.dto.WorkerWorkCaseResponse;

public interface WorkerWorkCaseService {

    WorkerHomeResponse home(AuthPrincipal principal);

    PageResponse<WorkerWorkCaseResponse> list(AuthPrincipal principal, int page, int size);
}
