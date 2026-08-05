package com.gighub.support;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.exception.AuthRequiredException;
import com.gighub.member.domain.User;
import com.gighub.member.domain.UserStatus;
import com.gighub.member.mapper.UserMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** local 프로파일에서만 사용하는 테스트 사용자 조회 경계입니다. */
@Service
@Profile("local")
public class TestLoginService {

    private final UserMapper userMapper;

    public TestLoginService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public AuthPrincipal loadActivePrincipal(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthRequiredException("로그인할 수 없는 테스트 사용자입니다.");
        }
        return new AuthPrincipal(user.getId(), user.getRole(), user.getName());
    }
}
