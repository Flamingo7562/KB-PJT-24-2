package com.gighub.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.gighub.auth.dto.SignupRequest;
import com.gighub.auth.exception.AuthErrorCode;
import com.gighub.auth.exception.AuthException;
import com.gighub.auth.exception.AuthValidationException;
import com.gighub.auth.service.impl.AuthServiceImpl;
import com.gighub.member.domain.User;
import com.gighub.member.mapper.UserMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    private SignupRequest validRequest() {
        SignupRequest request = new SignupRequest();
        request.setLoginId(" tester01 ");
        request.setPassword("abcd1234");
        request.setPasswordConfirm("abcd1234");
        request.setName(" 김테스트 ");
        request.setEmail(" Tester01@Example.com ");
        request.setPhone("010-1234-5678");
        request.setRole("WORKER");
        return request;
    }

    private AuthService newService() {
        return new AuthServiceImpl(userMapper, passwordEncoder);
    }

    @Test
    void loginIdAvailableWhenCountIsZero() {
        when(userMapper.countByLoginId("free")).thenReturn(0);
        authService = newService();

        assertTrue(authService.isLoginIdAvailable("  free  "));
    }

    @Test
    void loginIdNotAvailableWhenCountIsPositive() {
        when(userMapper.countByLoginId("taken")).thenReturn(1);
        authService = newService();

        assertFalse(authService.isLoginIdAvailable("taken"));
    }

    @Test
    void emailAvailabilityNormalizesBeforeChecking() {
        when(userMapper.countByEmail("user@test.com")).thenReturn(0);
        authService = newService();

        assertTrue(authService.isEmailAvailable("  USER@Test.COM  "));
    }

    @Test
    void signupNormalizesHashesAndInsertsUser() {
        when(userMapper.countByLoginId("tester01")).thenReturn(0);
        when(userMapper.countByEmail("tester01@example.com")).thenReturn(0);
        when(passwordEncoder.encode("abcd1234")).thenReturn("{bcrypt}hashed");
        authService = newService();

        authService.signup(validRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userMapper).insert(captor.capture());

        User inserted = captor.getValue();
        assertEquals("tester01", inserted.getLoginId());
        assertEquals("tester01@example.com", inserted.getEmail());
        assertEquals("김테스트", inserted.getName());
        assertEquals("01012345678", inserted.getPhone());
        assertEquals("WORKER", inserted.getRole());
        assertEquals("{bcrypt}hashed", inserted.getPasswordHash());
    }

    @Test
    void signupRejectsMismatchedPasswordConfirm() {
        SignupRequest request = validRequest();
        request.setPasswordConfirm("different1");
        authService = newService();

        AuthValidationException exception =
                assertThrows(AuthValidationException.class, () -> authService.signup(request));

        assertEquals("passwordConfirm", exception.getFieldErrors().get(0).getField());
    }

    @Test
    void signupRejectsInvalidPhoneFormatAfterNormalization() {
        SignupRequest request = validRequest();
        request.setPhone("123-456");
        authService = newService();

        AuthValidationException exception =
                assertThrows(AuthValidationException.class, () -> authService.signup(request));

        assertEquals("phone", exception.getFieldErrors().get(0).getField());
    }

    @Test
    void signupRejectsDuplicateLoginId() {
        when(userMapper.countByLoginId("tester01")).thenReturn(1);
        authService = newService();

        AuthException exception = assertThrows(AuthException.class, () -> authService.signup(validRequest()));

        assertEquals(AuthErrorCode.LOGIN_ID_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    void signupRejectsDuplicateEmail() {
        when(userMapper.countByLoginId("tester01")).thenReturn(0);
        when(userMapper.countByEmail("tester01@example.com")).thenReturn(1);
        authService = newService();

        AuthException exception = assertThrows(AuthException.class, () -> authService.signup(validRequest()));

        assertEquals(AuthErrorCode.EMAIL_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    void signupTranslatesRaceConditionDuplicateKeyOnLoginId() {
        when(userMapper.countByLoginId("tester01")).thenReturn(0);
        when(userMapper.countByEmail("tester01@example.com")).thenReturn(0);
        when(passwordEncoder.encode(anyString())).thenReturn("{bcrypt}hashed");
        when(userMapper.insert(any(User.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry 'tester01' for key 'uk_users_login_id'"));
        authService = newService();

        AuthException exception = assertThrows(AuthException.class, () -> authService.signup(validRequest()));

        assertEquals(AuthErrorCode.LOGIN_ID_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    void signupTranslatesRaceConditionDuplicateKeyOnEmail() {
        when(userMapper.countByLoginId("tester01")).thenReturn(0);
        when(userMapper.countByEmail("tester01@example.com")).thenReturn(0);
        when(passwordEncoder.encode(anyString())).thenReturn("{bcrypt}hashed");
        when(userMapper.insert(any(User.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry 'x' for key 'uk_users_email'"));
        authService = newService();

        AuthException exception = assertThrows(AuthException.class, () -> authService.signup(validRequest()));

        assertEquals(AuthErrorCode.EMAIL_ALREADY_EXISTS, exception.getErrorCode());
    }
}
