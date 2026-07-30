package com.gighub.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 전화번호는 선택 항목이라 형식 검사를 이 DTO에서 하지 않는다 — 정규화(하이픈 제거) 후
 * {@code AuthService}가 검사한다. passwordConfirm 일치 여부도 서비스에서 확인한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class SignupRequest {

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9]{4,20}$", message = "아이디는 4~20자 영문·숫자입니다.")
    private String loginId;

    @NotBlank
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$",
            message = "비밀번호는 8~64자이며 영문과 숫자를 모두 포함해야 합니다.")
    private String password;

    @NotBlank
    private String passwordConfirm;

    @NotBlank
    private String name;

    @NotBlank
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    private String phone;

    @NotBlank
    @Pattern(regexp = "OWNER|WORKER", message = "역할은 OWNER 또는 WORKER여야 합니다.")
    private String role;
}
