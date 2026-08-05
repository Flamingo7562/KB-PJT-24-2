package com.gighub.auth.service;

import com.gighub.auth.security.AuthPrincipal;

/** 가입·로그인과 Session 응답에 필요한 인증 도메인 기능입니다. */
public interface AuthService {

    boolean needsWorkplaceSetup(AuthPrincipal principal);
}
