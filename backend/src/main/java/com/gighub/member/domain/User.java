package com.gighub.member.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * users 테이블 행을 옮기는 영속 VO. password_hash를 포함하므로 응답 DTO로
 * 그대로 노출하지 않는다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String loginId;
    private String email;
    private String passwordHash;
    private String name;
    private String phone;
    private String role;
    private String status;
}
