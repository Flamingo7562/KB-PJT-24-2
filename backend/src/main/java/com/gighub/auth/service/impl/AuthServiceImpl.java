package com.gighub.auth.service.impl;

import com.gighub.auth.dto.SignupRequest;
import com.gighub.auth.mapper.WorkplaceCountMapper;
import com.gighub.auth.security.AuthPrincipal;
import com.gighub.auth.service.AuthService;
import com.gighub.common.exception.ConflictException;
import com.gighub.member.domain.User;
import com.gighub.member.domain.UserRole;
import com.gighub.member.mapper.UserMapper;
import com.gighub.wallet.mapper.WalletMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 승인된 인증 계약을 DB 현재 상태로 계산합니다. */
@Service
public class AuthServiceImpl implements AuthService {

    private final WorkplaceCountMapper workplaceCountMapper;
    private final UserMapper userMapper;
    private final WalletMapper walletMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(
            WorkplaceCountMapper workplaceCountMapper,
            UserMapper userMapper,
            WalletMapper walletMapper,
            PasswordEncoder passwordEncoder) {
        this.workplaceCountMapper = workplaceCountMapper;
        this.userMapper = userMapper;
        this.walletMapper = walletMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean isLoginIdAvailable(String loginId) {
        return userMapper.countByLoginId(loginId) == 0;
    }

    @Override
    public boolean isEmailAvailable(String email) {
        return userMapper.countByEmail(email) == 0;
    }

    @Override
    @Transactional
    public Long signup(SignupRequest request) {
        if (!isLoginIdAvailable(request.getLoginId())
                || !isEmailAvailable(request.getEmail())) {
            throw signupConflict();
        }

        User user = new User(
                request.getLoginId(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getName(),
                request.getPhone(),
                request.getRole()
        );

        try {
            if (userMapper.insert(user) != 1 || user.getId() == null) {
                throw new IllegalStateException("가입 사용자 저장 결과가 올바르지 않습니다.");
            }
            // 사용자와 기본 지갑은 하나의 가입 단위이므로 어느 한쪽 실패 시 함께 되돌립니다.
            if (walletMapper.insertKrwWallet(user.getId()) != 1) {
                throw new IllegalStateException("가입 지갑 저장 결과가 올바르지 않습니다.");
            }
            return user.getId();
        } catch (DuplicateKeyException exception) {
            // 사전 조회 이후 동시 가입이 들어와도 DB Unique 제약을 최종 방어선으로 사용합니다.
            throw signupConflict();
        }
    }

    @Override
    public boolean needsWorkplaceSetup(AuthPrincipal principal) {
        if (principal.getRole() != UserRole.OWNER) {
            return false;
        }
        // 사업장 생성·비활성화가 즉시 반영되도록 Session에 계산 결과를 저장하지 않습니다.
        return workplaceCountMapper.countActiveByOwnerUserId(principal.getUserId()) == 0;
    }

    private ConflictException signupConflict() {
        return new ConflictException("이미 사용 중인 가입 정보입니다.");
    }
}
