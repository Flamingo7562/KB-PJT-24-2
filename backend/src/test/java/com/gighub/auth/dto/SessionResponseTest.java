package com.gighub.auth.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void unauthenticatedOmitsOptionalFields() throws Exception {
        String json = objectMapper.writeValueAsString(SessionResponse.unauthenticated());

        assertTrue(json.contains("\"authenticated\":false"));
        assertFalse(json.contains("role"));
        assertFalse(json.contains("needsWorkplaceSetup"));
    }

    @Test
    void authenticatedIncludesAllFields() throws Exception {
        String json = objectMapper.writeValueAsString(SessionResponse.authenticated("OWNER", "김사장", true));

        assertTrue(json.contains("\"authenticated\":true"));
        assertTrue(json.contains("\"role\":\"OWNER\""));
        assertTrue(json.contains("\"name\":\"김사장\""));
        assertTrue(json.contains("\"needsWorkplaceSetup\":true"));
    }
}
