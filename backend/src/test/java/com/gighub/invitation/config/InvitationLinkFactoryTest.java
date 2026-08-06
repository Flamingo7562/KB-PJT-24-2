package com.gighub.invitation.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 초대 URL이 설정 Origin에서만 만들어지는지 확인합니다.
 */
class InvitationLinkFactoryTest {

    private static final String SECRET = "local-invitation-secret-value-0123456789";
    private static final String TOKEN = "3rXQ0Zk8m1UvJ2Nw6bTyaPcLdEfGhIjKlMnOpQrStUv";

    @Test
    void buildsAbsoluteInvitationUrlFromConfiguredOrigin() {
        InvitationLinkFactory factory = new InvitationLinkFactory(
                InvitationProperties.of(SECRET, null, "https://app.example.com")
        );

        assertEquals("https://app.example.com/invitations/" + TOKEN, factory.toInviteUrl(TOKEN));
    }

    @Test
    void trailingSlashInConfigurationDoesNotDuplicateThePathSeparator() {
        InvitationLinkFactory factory = new InvitationLinkFactory(
                InvitationProperties.of(SECRET, null, "https://app.example.com/")
        );

        assertEquals("https://app.example.com/invitations/" + TOKEN, factory.toInviteUrl(TOKEN));
    }

    @Test
    void blankTokenIsRejected() {
        InvitationLinkFactory factory = new InvitationLinkFactory(
                InvitationProperties.of(SECRET, null, "https://app.example.com")
        );

        assertThrows(IllegalArgumentException.class, () -> factory.toInviteUrl(null));
        assertThrows(IllegalArgumentException.class, () -> factory.toInviteUrl("  "));
    }
}
