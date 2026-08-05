package com.gighub.auth.service;

import com.gighub.auth.mapper.WorkplaceCountMapper;
import com.gighub.auth.security.AuthPrincipal;
import com.gighub.auth.service.impl.AuthServiceImpl;
import com.gighub.member.domain.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private final WorkplaceCountMapper mapper = mock(WorkplaceCountMapper.class);
    private final AuthServiceImpl service = new AuthServiceImpl(mapper);

    @Test
    void ownerWithoutActiveWorkplaceNeedsSetup() {
        AuthPrincipal principal = new AuthPrincipal(1L, UserRole.OWNER, "김사장");
        when(mapper.countActiveByOwnerUserId(1L)).thenReturn(0);

        assertTrue(service.needsWorkplaceSetup(principal));
    }

    @Test
    void ownerWithActiveWorkplaceDoesNotNeedSetup() {
        AuthPrincipal principal = new AuthPrincipal(1L, UserRole.OWNER, "김사장");
        when(mapper.countActiveByOwnerUserId(1L)).thenReturn(1);

        assertFalse(service.needsWorkplaceSetup(principal));
    }

    @Test
    void workerNeverQueriesWorkplaceCount() {
        AuthPrincipal principal = new AuthPrincipal(2L, UserRole.WORKER, "김근로");

        assertFalse(service.needsWorkplaceSetup(principal));
        verify(mapper, never()).countActiveByOwnerUserId(2L);
    }
}
