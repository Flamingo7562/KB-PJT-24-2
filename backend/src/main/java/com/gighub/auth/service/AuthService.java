package com.gighub.auth.service;

import com.gighub.auth.dto.SignupRequest;

public interface AuthService {

    boolean isLoginIdAvailable(String loginId);

    boolean isEmailAvailable(String email);

    Long signup(SignupRequest request);
}
