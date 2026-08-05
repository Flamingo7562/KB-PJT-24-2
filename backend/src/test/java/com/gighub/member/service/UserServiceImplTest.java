package com.gighub.member.service;

import com.gighub.common.exception.ResourceNotFoundException;
import com.gighub.member.domain.User;
import com.gighub.member.domain.UserRole;
import com.gighub.member.domain.UserStatus;
import com.gighub.member.dto.UserProfileResponse;
import com.gighub.member.mapper.UserMapper;
import com.gighub.member.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final UserServiceImpl service = new UserServiceImpl(userMapper);

    @Test
    void returnsApprovedProfileFieldsForOwner() {
        when(userMapper.findProfileById(42L)).thenReturn(owner());

        UserProfileResponse response = service.getProfile(42L);

        assertEquals("owner01", response.getLoginId());
        assertEquals("owner@example.com", response.getEmail());
        assertEquals("김사장", response.getName());
        assertEquals("01012345678", response.getPhone());
        assertEquals(UserRole.OWNER, response.getRole());
        assertEquals(UserStatus.ACTIVE, response.getStatus());
    }

    @Test
    void returnsProfileForWorker() {
        User worker = owner();
        worker.setLoginId("worker01");
        worker.setRole(UserRole.WORKER);
        when(userMapper.findProfileById(43L)).thenReturn(worker);

        UserProfileResponse response = service.getProfile(43L);

        assertEquals("worker01", response.getLoginId());
        assertEquals(UserRole.WORKER, response.getRole());
    }

    @Test
    void keepsPhoneNullWhenNotRegistered() {
        User user = owner();
        user.setPhone(null);
        when(userMapper.findProfileById(42L)).thenReturn(user);

        assertNull(service.getProfile(42L).getPhone());
    }

    @Test
    void rejectsMissingUser() {
        when(userMapper.findProfileById(99L)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> service.getProfile(99L));
    }

    private User owner() {
        User user = new User();
        user.setId(42L);
        user.setLoginId("owner01");
        user.setEmail("owner@example.com");
        user.setName("김사장");
        user.setPhone("01012345678");
        user.setRole(UserRole.OWNER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}