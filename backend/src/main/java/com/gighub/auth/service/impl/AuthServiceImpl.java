package com.gighub.auth.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gighub.auth.AuthNormalizer;
import com.gighub.auth.dto.SignupRequest;
import com.gighub.auth.exception.AuthErrorCode;
import com.gighub.auth.exception.AuthException;
import com.gighub.auth.exception.AuthValidationException;
import com.gighub.auth.exception.FieldErrorItem;
import com.gighub.auth.service.AuthService;
import com.gighub.member.domain.User;
import com.gighub.member.mapper.UserMapper;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{8,10}$");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean isLoginIdAvailable(String loginId) {
        return userMapper.countByLoginId(AuthNormalizer.loginId(loginId)) == 0;
    }

    @Override
    public boolean isEmailAvailable(String email) {
        return userMapper.countByEmail(AuthNormalizer.email(email)) == 0;
    }

    @Override
    public Long signup(SignupRequest request) {
        String loginId = AuthNormalizer.loginId(request.getLoginId());
        String email = AuthNormalizer.email(request.getEmail());
        String name = AuthNormalizer.name(request.getName());
        String phone = AuthNormalizer.phone(request.getPhone());

        validateCrossFieldRules(request, phone);

        if (userMapper.countByLoginId(loginId) > 0) {
            throw new AuthException(AuthErrorCode.LOGIN_ID_ALREADY_EXISTS);
        }
        if (userMapper.countByEmail(email) > 0) {
            throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .loginId(loginId)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(name)
                .phone(phone)
                .role(request.getRole())
                .build();

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw translateDuplicateKey(e);
        }

        return user.getId();
    }

    private void validateCrossFieldRules(SignupRequest request, String normalizedPhone) {
        List<FieldErrorItem> errors = new ArrayList<>();

        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            errors.add(new FieldErrorItem("passwordConfirm", "비밀번호가 일치하지 않습니다."));
        }
        if (normalizedPhone != null && !PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            errors.add(new FieldErrorItem("phone", "올바른 전화번호 형식이 아닙니다."));
        }

        if (!errors.isEmpty()) {
            throw new AuthValidationException(errors);
        }
    }

    private AuthException translateDuplicateKey(DuplicateKeyException e) {
        String message = String.valueOf(e.getMostSpecificCause().getMessage());
        if (message.contains("uk_users_login_id")) {
            return new AuthException(AuthErrorCode.LOGIN_ID_ALREADY_EXISTS);
        }
        if (message.contains("uk_users_email")) {
            return new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }
        return new AuthException(AuthErrorCode.VALIDATION_FAILED, "중복된 값이 있습니다.");
    }
}
