package com.gighub.invitation.config;

import org.springframework.stereotype.Component;

/**
 * 초대 Token 원문을 응답에 담을 절대 초대 URL로 조립합니다.
 *
 * <p>주소는 요청 Header가 아니라 배포 설정의 Web Origin에서만 만듭니다. Header를 신뢰하면
 * 다른 Host를 넣은 요청이 그대로 초대 Link에 실려 나갈 수 있습니다.</p>
 */
@Component
public class InvitationLinkFactory {

    private static final String INVITATION_PATH = "/invitations/";

    private final InvitationProperties properties;

    public InvitationLinkFactory(InvitationProperties properties) {
        this.properties = properties;
    }

    /**
     * @param token 초대 Token 원문
     * @return Query·Fragment 없는 절대 초대 URL
     */
    public String toInviteUrl(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token");
        }
        return properties.getWebOrigin() + INVITATION_PATH + token;
    }
}
