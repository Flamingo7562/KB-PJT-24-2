package com.gighub.auth.service;

import com.gighub.auth.dto.SignupRequest;
import com.gighub.auth.mapper.WorkplaceCountMapper;
import com.gighub.auth.security.AuthPrincipal;
import com.gighub.auth.service.impl.AuthServiceImpl;
import com.gighub.common.exception.ConflictException;
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
}
