package com.gighub.member.service.impl;

import com.gighub.common.exception.ResourceNotFoundException;
import com.gighub.member.domain.User;
import com.gighub.member.dto.UserProfileResponse;
import com.gighub.member.mapper.UserMapper;
import com.gighub.member.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 인증 Principal의 userId만으로 본인 프로필을 다룹니다. */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = userMapper.findProfileById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("사용자를 찾을 수 없습니다.");
        }
        return UserProfileResponse.from(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updatePhone(Long userId, String phone) {
        int updated = userMapper.updatePhone(userId, phone);
        if (updated == 0) {
            throw new ResourceNotFoundException("사용자를 찾을 수 없습니다.");
        }
        return UserProfileResponse.from(userMapper.findProfileById(userId));
    }
}