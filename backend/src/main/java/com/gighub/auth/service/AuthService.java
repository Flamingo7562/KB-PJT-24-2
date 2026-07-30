package com.gighub.auth.service;

import com.gighub.auth.dto.LoginRequest;
import com.gighub.auth.dto.SignupRequest;
import com.gighub.auth.security.AuthPrincipal;

public interface AuthService {

    boolean isLoginIdAvailable(String loginId);

    boolean isEmailAvailable(String email);

    Long signup(SignupRequest request);

    AuthPrincipal authenticate(LoginRequest request);

    boolean needsWorkplaceSetup(AuthPrincipal principal);
}
