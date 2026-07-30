package com.gighub.member.mapper;

import com.gighub.member.domain.User;

public interface UserMapper {

    int countByLoginId(String loginId);

    int countByEmail(String email);

    int insert(User user);
}
