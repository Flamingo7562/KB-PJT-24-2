package com.gighub.support;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.exception.AuthRequiredException;
import com.gighub.member.domain.User;
import com.gighub.member.domain.UserRole;
import com.gighub.member.domain.UserStatus;
import com.gighub.member.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestLoginServiceTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final TestLoginService service = new TestLoginService(userMapper);

    @Test
    void loadsOnlyActiveUserAsSecurityPrincipal() {
        User user = user(UserStatus.ACTIVE);
        when(userMapper.findById(71L)).thenReturn(user);

        AuthPrincipal principal = service.loadActivePrincipal(71L);

        assertEquals(71L, principal.getUserId());
        assertEquals(UserRole.WORKER, principal.getRole());
        assertEquals("로컬 사용자", principal.getName());
    }

    @Test
    void rejectsInactiveLocalTestUser() {
        when(userMapper.findById(71L)).thenReturn(user(UserStatus.INACTIVE));

        assertThrows(AuthRequiredException.class, () -> service.loadActivePrincipal(71L));
    }

    private User user(UserStatus status) {
        User user = new User();
        user.setId(71L);
        user.setName("로컬 사용자");
        user.setRole(UserRole.WORKER);
        user.setStatus(status);
        return user;
    }
}
