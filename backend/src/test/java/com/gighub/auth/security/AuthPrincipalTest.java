package com.gighub.auth.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthPrincipalTest {

    @Test
    void exposesUserIdAndRole() {
        AuthPrincipal principal = new AuthPrincipal(1L, "OWNER");

        assertEquals(1L, principal.getUserId());
        assertEquals("OWNER", principal.getRole());
    }

    @Test
    void equalsComparesByValue() {
        assertEquals(new AuthPrincipal(1L, "OWNER"), new AuthPrincipal(1L, "OWNER"));
        assertNotEquals(new AuthPrincipal(1L, "OWNER"), new AuthPrincipal(1L, "WORKER"));
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new AuthPrincipal(null, "OWNER"));
        assertThrows(NullPointerException.class, () -> new AuthPrincipal(1L, null));
    }
}
