package com.gighub.workplace.service;

import java.math.BigDecimal;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.exception.ConflictException;
import com.gighub.common.exception.ForbiddenException;
import com.gighub.member.domain.UserRole;
import com.gighub.workplace.mapper.WorkplaceMapper;
import com.gighub.workplace.mapper.param.WorkplaceInsertParam;
import com.gighub.workplace.service.command.WorkplaceCreateCommand;
import com.gighub.workplace.service.impl.WorkplaceServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class WorkplaceServiceImplTest {

    private final WorkplaceMapper workplaceMapper = mock(WorkplaceMapper.class);
    private final WorkplaceServiceImpl service = new WorkplaceServiceImpl(workplaceMapper);

    @Test
    void storesOwnerFromPrincipalAndReturnsGeneratedIdentifier() {
        doAnswer(invocation -> {
            invocation.getArgument(0, WorkplaceInsertParam.class).setId(42L);
            return 1;
        }).when(workplaceMapper).insert(any(WorkplaceInsertParam.class));

        Long workplaceId = service.create(owner(7L), validCommand());

        assertEquals(42L, workplaceId);

        ArgumentCaptor<WorkplaceInsertParam> captor =
                ArgumentCaptor.forClass(WorkplaceInsertParam.class);
        verify(workplaceMapper).insert(captor.capture());

        WorkplaceInsertParam param = captor.getValue();
        assertEquals(7L, param.getOwnerUserId());
        assertEquals("1234567890", param.getBusinessRegistrationNumber());
        assertEquals("강남점", param.getName());
        assertEquals("김사장", param.getRepresentativeName());
        assertEquals("서울 강남구 테헤란로 1", param.getRoadAddress());
        assertEquals("2층", param.getDetailAddress());
        assertEquals("0212345678", param.getPhone());
        assertEquals(0, new BigDecimal("37.1234567").compareTo(param.getLatitude()));
        assertEquals(0, new BigDecimal("127.1234567").compareTo(param.getLongitude()));
    }

    @Test
    void rejectsNonOwnerBeforeTouchingStorage() {
        AuthPrincipal worker = new AuthPrincipal(9L, UserRole.WORKER, "김근로");

        assertThrows(ForbiddenException.class, () -> service.create(worker, validCommand()));
        verify(workplaceMapper, never()).insert(any(WorkplaceInsertParam.class));
    }

    @Test
    void translatesUniqueViolationIntoApprovedConflict() {
        doThrow(new DuplicateKeyException("uk_workplaces_business_registration_number"))
                .when(workplaceMapper).insert(any(WorkplaceInsertParam.class));

        assertThrows(ConflictException.class, () -> service.create(owner(7L), validCommand()));
    }

    /** 좌표는 선택값이므로 없는 요청도 그대로 저장 파라미터에 전달돼야 합니다. */
    @Test
    void keepsOptionalValuesAbsentInsteadOfSubstituting() {
        doAnswer(invocation -> {
            invocation.getArgument(0, WorkplaceInsertParam.class).setId(43L);
            return 1;
        }).when(workplaceMapper).insert(any(WorkplaceInsertParam.class));

        service.create(owner(7L), WorkplaceCreateCommand.builder()
                .businessRegistrationNumber("1234567890")
                .name("강남점")
                .representativeName("김사장")
                .roadAddress("서울 강남구 테헤란로 1")
                .phone("0212345678")
                .build());

        ArgumentCaptor<WorkplaceInsertParam> captor =
                ArgumentCaptor.forClass(WorkplaceInsertParam.class);
        verify(workplaceMapper).insert(captor.capture());

        WorkplaceInsertParam param = captor.getValue();
        assertEquals(null, param.getDetailAddress());
        assertEquals(null, param.getLatitude());
        assertEquals(null, param.getLongitude());
    }

    private AuthPrincipal owner(Long userId) {
        return new AuthPrincipal(userId, UserRole.OWNER, "김사장");
    }

    private WorkplaceCreateCommand validCommand() {
        return WorkplaceCreateCommand.builder()
                .businessRegistrationNumber("1234567890")
                .name("강남점")
                .representativeName("김사장")
                .roadAddress("서울 강남구 테헤란로 1")
                .detailAddress("2층")
                .phone("0212345678")
                .latitude(new BigDecimal("37.1234567"))
                .longitude(new BigDecimal("127.1234567"))
                .build();
    }
}
