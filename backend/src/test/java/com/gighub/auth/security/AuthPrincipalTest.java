package com.gighub.auth.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthPrincipalTest {

    @Test
    void exposesUserIdRoleAndName() {
        AuthPrincipal principal = new AuthPrincipal(1L, "OWNER", "김사장");

        assertEquals(1L, principal.getUserId());
        assertEquals("OWNER", principal.getRole());
        assertEquals("김사장", principal.getName());
    }

    @Test
    void equalsComparesByValue() {
        assertEquals(new AuthPrincipal(1L, "OWNER", "김사장"), new AuthPrincipal(1L, "OWNER", "김사장"));
        assertNotEquals(new AuthPrincipal(1L, "OWNER", "김사장"), new AuthPrincipal(1L, "WORKER", "김사장"));
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new AuthPrincipal(null, "OWNER", "김사장"));
        assertThrows(NullPointerException.class, () -> new AuthPrincipal(1L, null, "김사장"));
        assertThrows(NullPointerException.class, () -> new AuthPrincipal(1L, "OWNER", null));
    }
}
