package com.gighub.auth.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gighub.member.domain.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void unauthenticatedResponseOmitsAuthenticatedOnlyFields() {
        JsonNode json = objectMapper.valueToTree(SessionResponse.unauthenticated());

        assertFalse(json.get("authenticated").asBoolean());
        assertEquals(1, json.size());
    }

    @Test
    void authenticatedResponseContainsApprovedFields() {
        JsonNode json = objectMapper.valueToTree(
                SessionResponse.authenticated(UserRole.OWNER, "김사장", true)
        );

        assertEquals(4, json.size());
        assertEquals("OWNER", json.get("role").asText());
        assertEquals("김사장", json.get("name").asText());
        assertTrue(json.get("needsWorkplaceSetup").asBoolean());
    }
}
