package com.gighub.auth.service;

import java.util.List;

import com.gighub.auth.dto.SignupRequest;
import com.gighub.auth.dto.LoginRequest;
import com.gighub.auth.mapper.WorkplaceCountMapper;
import com.gighub.auth.security.AuthPrincipal;
import com.gighub.auth.service.impl.AuthServiceImpl;
import com.gighub.common.exception.ConflictException;
import com.gighub.common.exception.AuthRequiredException;
import com.gighub.common.exception.RoleMismatchException;
import com.gighub.member.domain.User;
import com.gighub.member.domain.UserRole;
import com.gighub.member.domain.UserStatus;
import com.gighub.member.mapper.UserMapper;
import com.gighub.wallet.mapper.WalletMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private final WorkplaceCountMapper workplaceCountMapper = mock(WorkplaceCountMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final WalletMapper walletMapper = mock(WalletMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuthServiceImpl service = new AuthServiceImpl(
            workplaceCountMapper,
            userMapper,
            walletMapper,
            passwordEncoder
    );

    @Test
    void ownerWithoutActiveWorkplaceNeedsSetup() {
        AuthPrincipal principal = new AuthPrincipal(1L, UserRole.OWNER, "김사장");
        when(workplaceCountMapper.countActiveByOwnerUserId(1L)).thenReturn(0);

        assertTrue(service.needsWorkplaceSetup(principal));
    }

    @Test
    void ownerWithActiveWorkplaceDoesNotNeedSetup() {
        AuthPrincipal principal = new AuthPrincipal(1L, UserRole.OWNER, "김사장");
        when(workplaceCountMapper.countActiveByOwnerUserId(1L)).thenReturn(1);

        assertFalse(service.needsWorkplaceSetup(principal));
    }

    @Test
    void workerNeverQueriesWorkplaceCount() {
        AuthPrincipal principal = new AuthPrincipal(2L, UserRole.WORKER, "김근로");

        assertFalse(service.needsWorkplaceSetup(principal));
        verify(workplaceCountMapper, never()).countActiveByOwnerUserId(2L);
    }

    @Test
    void availabilityReadsNormalizedIdentityCounts() {
        when(userMapper.countByLoginId("worker01")).thenReturn(0);
        when(userMapper.countByEmail("used@example.com")).thenReturn(1);

        assertTrue(service.isLoginIdAvailable("worker01"));
        assertFalse(service.isEmailAvailable("used@example.com"));
    }

    @Test
    void signupCreatesActiveUserAndKrwWallet() {
        SignupRequest request = signupRequest();
        when(passwordEncoder.encode("secret123")).thenReturn("bcrypt-hash");
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(31L);
            return 1;
        });
        when(walletMapper.insertKrwWallet(31L)).thenReturn(1);

        assertEquals(31L, service.signup(request));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User user = userCaptor.getValue();
        assertEquals("worker01", user.getLoginId());
        assertEquals("worker@example.com", user.getEmail());
        assertEquals("bcrypt-hash", user.getPasswordHash());
        assertEquals("01012345678", user.getPhone());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(walletMapper).insertKrwWallet(31L);
    }

    @Test
    void signupRejectsKnownDuplicateBeforeHashing() {
        SignupRequest request = signupRequest();
        when(userMapper.countByLoginId("worker01")).thenReturn(1);

        assertThrows(ConflictException.class, () -> service.signup(request));

        verify(passwordEncoder, never()).encode(any());
        verify(userMapper, never()).insert(any());
    }

    @Test
    void signupConvertsConcurrentUniqueViolationToConflict() {
        SignupRequest request = signupRequest();
        when(passwordEncoder.encode("secret123")).thenReturn("bcrypt-hash");
        when(userMapper.insert(any())).thenThrow(new DuplicateKeyException("duplicate"));

        assertThrows(ConflictException.class, () -> service.signup(request));

        verify(walletMapper, never()).insertKrwWallet(any());
    }

    @Test
    void signupFailsWhenWalletIsNotCreated() {
        SignupRequest request = signupRequest();
        when(passwordEncoder.encode("secret123")).thenReturn("bcrypt-hash");
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(32L);
            return 1;
        });
        when(walletMapper.insertKrwWallet(32L)).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> service.signup(request));
    }

    @Test
    void activeOwnerLoginReturnsPrincipalAndLiveWorkplaceState() {
        LoginRequest request = loginRequest(UserRole.OWNER);
        User user = user(51L, UserRole.OWNER, UserStatus.ACTIVE);
        when(userMapper.findByLoginId("owner01")).thenReturn(user);
        when(passwordEncoder.matches("secret123", "bcrypt-hash")).thenReturn(true);
        when(workplaceCountMapper.countActiveByOwnerUserId(51L)).thenReturn(0);

        LoginResult result = service.login(request);

        assertEquals(51L, result.getPrincipal().getUserId());
        assertEquals(UserRole.OWNER, result.getPrincipal().getRole());
        assertEquals("테스트 사용자", result.getPrincipal().getName());
        assertTrue(result.isNeedsWorkplaceSetup());
    }

    @Test
    void unknownLoginIdUsesDummyHashAndReturnsAuthRequired() {
        LoginRequest request = loginRequest(UserRole.OWNER);

        assertThrows(AuthRequiredException.class, () -> service.login(request));

        verify(passwordEncoder).matches(org.mockito.ArgumentMatchers.eq("secret123"), anyString());
    }

    @Test
    void wrongPasswordReturnsSameAuthRequiredError() {
        LoginRequest request = loginRequest(UserRole.OWNER);
        when(userMapper.findByLoginId("owner01"))
                .thenReturn(user(52L, UserRole.OWNER, UserStatus.ACTIVE));
        when(passwordEncoder.matches("secret123", "bcrypt-hash")).thenReturn(false);

        assertThrows(AuthRequiredException.class, () -> service.login(request));
    }

    @Test
    void everyNonActiveAccountReturnsSameAuthRequiredError() {
        LoginRequest request = loginRequest(UserRole.OWNER);
        when(passwordEncoder.matches("secret123", "bcrypt-hash")).thenReturn(true);

        for (UserStatus status : List.of(
                UserStatus.INACTIVE,
                UserStatus.LOCKED,
                UserStatus.WITHDRAWN)) {
            when(userMapper.findByLoginId("owner01"))
                    .thenReturn(user(53L, UserRole.OWNER, status));
            assertThrows(AuthRequiredException.class, () -> service.login(request));
        }
    }

    @Test
    void validCredentialsWithDifferentRoleReturnRoleMismatch() {
        LoginRequest request = loginRequest(UserRole.WORKER);
        when(userMapper.findByLoginId("owner01"))
                .thenReturn(user(54L, UserRole.OWNER, UserStatus.ACTIVE));
        when(passwordEncoder.matches("secret123", "bcrypt-hash")).thenReturn(true);

        assertThrows(RoleMismatchException.class, () -> service.login(request));

        verify(workplaceCountMapper, never()).countActiveByOwnerUserId(any());
    }

    private SignupRequest signupRequest() {
        SignupRequest request = new SignupRequest();
        request.setLoginId("worker01");
        request.setPassword("secret123");
        request.setPasswordConfirm("secret123");
        request.setName("김근로");
        request.setEmail("worker@example.com");
        request.setPhone("010-1234-5678");
        request.setRole(UserRole.WORKER);
        return request;
    }

    private LoginRequest loginRequest(UserRole expectedRole) {
        LoginRequest request = new LoginRequest();
        request.setLoginId("owner01");
        request.setPassword("secret123");
        request.setExpectedRole(expectedRole);
        return request;
    }

    private User user(Long id, UserRole role, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setLoginId("owner01");
        user.setPasswordHash("bcrypt-hash");
        user.setName("테스트 사용자");
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
