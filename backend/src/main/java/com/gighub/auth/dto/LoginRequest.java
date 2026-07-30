package com.gighub.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank
    private String loginId;

    @NotBlank
    private String password;

    @NotBlank
    @Pattern(regexp = "OWNER|WORKER", message = "역할은 OWNER 또는 WORKER여야 합니다.")
    private String expectedRole;
}
