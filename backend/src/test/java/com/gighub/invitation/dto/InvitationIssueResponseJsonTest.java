package com.gighub.invitation.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gighub.common.api.ApiResponse;
import com.gighub.config.ApiJsonMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 발급 응답이 승인 명세의 필드 집합과 표현으로 직렬화되는지 확인합니다.
 */
class InvitationIssueResponseJsonTest {

    private static final String TOKEN = "3rXQ0Zk8m1UvJ2Nw6bTyaPcLdEfGhIjKlMnOpQrStUv";

    private final ObjectMapper objectMapper = ApiJsonMapper.create();

    @Test
    void serializesInviteUrlAndUtcExpiry() throws Exception {
        JsonNode data = data(InvitationIssueResponse.of(
                "https://app.example.com/invitations/" + TOKEN,
                Instant.parse("2026-08-20T01:00:00Z")));

        assertEquals(
                "https://app.example.com/invitations/" + TOKEN,
                data.get("inviteUrl").asText());
        assertEquals("2026-08-20T01:00:00Z", data.get("expiresAt").asText());
        assertEquals(2, data.size(), "승인된 필드 수와 같아야 합니다.");
    }

    @Test
    void rawTokenIsCarriedOnlyInsideTheUrl() throws Exception {
        JsonNode data = data(InvitationIssueResponse.of(
                "https://app.example.com/invitations/" + TOKEN,
                Instant.parse("2026-08-20T01:00:00Z")));

        // 별도 token 필드를 두면 저장소에 Hash만 남긴 의미가 흐려집니다.
        assertFalse(data.has("token"));
        assertFalse(data.has("tokenHash"));
        assertFalse(data.has("invitationId"));
        assertFalse(data.has("workCaseId"));
    }

    private JsonNode data(InvitationIssueResponse response) throws Exception {
        return objectMapper
                .readTree(objectMapper.writeValueAsString(ApiResponse.of(response)))
                .get("data");
    }
}
