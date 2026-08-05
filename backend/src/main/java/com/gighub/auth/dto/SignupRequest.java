package com.gighub.auth.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.gighub.auth.validation.PasswordMatches;
import com.gighub.auth.validation.Utf8ByteLength;
import com.gighub.member.domain.UserRole;

/** 승인된 가입 입력과 정규화·검증 계약입니다. */
@PasswordMatches
public class SignupRequest {

    @NotBlank(message = "로그인 아이디는 필수입니다.")
    @Size(max = 50, message = "로그인 아이디는 50자 이하여야 합니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
    @Utf8ByteLength(max = 72, message = "비밀번호는 UTF-8 기준 72byte 이하여야 합니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수입니다.")
    @Size(min = 8, max = 64, message = "비밀번호 확인은 8자 이상 64자 이하여야 합니다.")
    @Utf8ByteLength(max = 72, message = "비밀번호 확인은 UTF-8 기준 72byte 이하여야 합니다.")
    private String passwordConfirm;

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 100, message = "이름은 100자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "이메일은 필수입니다.")
    @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
    private String email;

    @Pattern(regexp = "^0\\d{8,10}$", message = "전화번호 형식이 올바르지 않습니다.")
    private String phone;

    @NotNull(message = "역할은 필수입니다.")
    private UserRole role;

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = AuthNormalizer.normalizeIdentity(loginId);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = AuthNormalizer.normalizeName(name);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = AuthNormalizer.normalizeIdentity(email);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = AuthNormalizer.normalizePhone(phone);
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    /** 명세에 없는 가입 필드는 조용히 무시하지 않고 요청 오류로 처리합니다. */
    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object value) {
        throw new IllegalArgumentException("허용되지 않은 가입 필드입니다: " + fieldName);
    }
}
