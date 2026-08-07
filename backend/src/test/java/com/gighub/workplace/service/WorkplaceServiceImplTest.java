package com.gighub.workplace.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.gighub.attendance.service.WorkplaceQrIssuer;
import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.api.PageResponse;
import com.gighub.common.exception.ConflictException;
import com.gighub.common.exception.RoleMismatchException;
import com.gighub.common.exception.ValidationException;
import com.gighub.member.domain.UserRole;
import com.gighub.workplace.dto.WorkplaceListItemResponse;
import com.gighub.workplace.mapper.WorkplaceMapper;
import com.gighub.workplace.mapper.param.WorkplaceInsertParam;
import com.gighub.workplace.mapper.result.WorkplaceListRow;
import com.gighub.workplace.mapper.result.WorkplaceCoordinateLockRow;
import com.gighub.workplace.service.command.WorkplaceCreateCommand;
import com.gighub.workplace.service.command.WorkplaceCoordinateConfirmCommand;
import com.gighub.workplace.service.impl.WorkplaceServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WorkplaceServiceImplTest {

    private final WorkplaceMapper workplaceMapper = mock(WorkplaceMapper.class);
    private final WorkplaceQrIssuer qrIssuer = mock(WorkplaceQrIssuer.class);
    private final WorkplaceServiceImpl service =
            new WorkplaceServiceImpl(workplaceMapper, qrIssuer);

    @Test
    void issuesFixedQrForTheNewWorkplaceWithinTheSameCall() {
        doAnswer(invocation -> {
            invocation.getArgument(0, WorkplaceInsertParam.class).setId(42L);
            return 1;
        }).when(workplaceMapper).insert(any(WorkplaceInsertParam.class));

        service.create(owner(7L), validCommand());

        verify(qrIssuer).issueActive(42L, 7L);
    }

    @Test
    void doesNotIssueQrWhenWorkplaceInsertFails() {
        doThrow(new DuplicateKeyException("duplicate"))
                .when(workplaceMapper).insert(any(WorkplaceInsertParam.class));

        assertThrows(ConflictException.class, () -> service.create(owner(7L), validCommand()));

        verifyNoInteractions(qrIssuer);
    }

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

        assertThrows(RoleMismatchException.class, () -> service.create(worker, validCommand()));
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

    /** 요청한 Page 값이 그대로 SQL 경계와 응답 Metadata에 반영돼야 합니다. */
    @Test
    void translatesRequestedPageIntoQueryBoundsAndMetadata() {
        when(workplaceMapper.countByOwnerUserId(7L)).thenReturn(3);
        when(workplaceMapper.findPageByOwnerUserId(7L, 2, 2L)).thenReturn(List.of(row(11L, "ACTIVE")));

        PageResponse<WorkplaceListItemResponse> response = service.findOwnedWorkplaces(owner(7L), 1, 2);

        verify(workplaceMapper).findPageByOwnerUserId(7L, 2, 2L);
        assertEquals(1, response.getPage().getNumber());
        assertEquals(2, response.getPage().getSize());
        assertEquals(3L, response.getPage().getTotalElements());
        assertEquals(2, response.getPage().getTotalPages());
        assertEquals(1, response.getContent().size());
    }

    /** 저장 정밀도(DECIMAL)와 상태 값이 승인된 응답 형태로 옮겨져야 합니다. */
    @Test
    void mapsRowColumnsIntoApprovedItemFields() {
        when(workplaceMapper.countByOwnerUserId(7L)).thenReturn(1);
        when(workplaceMapper.findPageByOwnerUserId(7L, 20, 0L)).thenReturn(List.of(row(11L, "INACTIVE")));

        WorkplaceListItemResponse item =
                service.findOwnedWorkplaces(owner(7L), 0, 20).getContent().get(0);

        assertEquals(11L, item.getWorkplaceId());
        assertEquals("1234567890", item.getBusinessRegistrationNumber());
        assertEquals("강남점", item.getName());
        assertEquals("김사장", item.getRepresentativeName());
        assertEquals("서울 강남구 테헤란로 1", item.getRoadAddress());
        assertEquals("2층", item.getDetailAddress());
        assertEquals("0212345678", item.getPhone());
        assertEquals(100, item.getRadiusMeters(), "DECIMAL(8,2)가 아니라 명세의 정수 100이어야 합니다.");
        assertTrue(item.isAttendanceLocationConfirmed());
        assertEquals("INACTIVE", item.getStatus(), "INACTIVE 사업장도 상태를 그대로 노출합니다.");
    }

    @Test
    void confirmsCoordinatesOnlyWhenOwnedActiveWorkplaceIsStillUnset() {
        when(workplaceMapper.findOwnedActiveCoordinatesForUpdate(11L, 7L))
                .thenReturn(new WorkplaceCoordinateLockRow(11L, null, null));
        when(workplaceMapper.setCoordinatesWhenAbsent(
                11L,
                7L,
                new BigDecimal("37.1234567"),
                new BigDecimal("127.1234567")))
                .thenReturn(1);

        service.confirmCoordinates(owner(7L), coordinateCommand(
                new BigDecimal("37.1234567"),
                new BigDecimal("127.1234567"),
                Instant.now()));

        verify(workplaceMapper).setCoordinatesWhenAbsent(
                11L,
                7L,
                new BigDecimal("37.1234567"),
                new BigDecimal("127.1234567"));
    }

    @Test
    void treatsSameConfirmedCoordinatesAsSuccessfulReplay() {
        when(workplaceMapper.findOwnedActiveCoordinatesForUpdate(11L, 7L))
                .thenReturn(new WorkplaceCoordinateLockRow(
                        11L,
                        new BigDecimal("37.1234567"),
                        new BigDecimal("127.1234567")));

        service.confirmCoordinates(owner(7L), coordinateCommand(
                new BigDecimal("37.12345670"),
                new BigDecimal("127.12345670"),
                Instant.now()));

        verify(workplaceMapper, never()).setCoordinatesWhenAbsent(
                anyLong(), anyLong(), any(), any());
    }

    @Test
    void rejectsDifferentCoordinatesAfterFirstConfirmation() {
        when(workplaceMapper.findOwnedActiveCoordinatesForUpdate(11L, 7L))
                .thenReturn(new WorkplaceCoordinateLockRow(
                        11L,
                        new BigDecimal("37.1234567"),
                        new BigDecimal("127.1234567")));

        assertThrows(
                WorkplaceCoordinatesAlreadySetException.class,
                () -> service.confirmCoordinates(owner(7L), coordinateCommand(
                        new BigDecimal("37.0000000"),
                        new BigDecimal("127.0000000"),
                        Instant.now())));
    }

    @Test
    void rejectsStaleCoordinateCaptureBeforeLockingWorkplace() {
        assertThrows(
                ValidationException.class,
                () -> service.confirmCoordinates(owner(7L), coordinateCommand(
                        new BigDecimal("37.1234567"),
                        new BigDecimal("127.1234567"),
                        Instant.now().minusSeconds(301))));

        verify(workplaceMapper, never()).findOwnedActiveCoordinatesForUpdate(anyLong(), anyLong());
    }

    @Test
    void rejectsNonOwnerBeforeQueryingList() {
        AuthPrincipal worker = new AuthPrincipal(9L, UserRole.WORKER, "김근로");

        assertThrows(RoleMismatchException.class, () -> service.findOwnedWorkplaces(worker, 0, 20));
        verifyNoInteractions(workplaceMapper);
    }

    /**
     * 권한 없는 호출자에게는 Page 규칙보다 역할 거절이 먼저입니다.
     *
     * <p>순서가 뒤집히면 WORKER가 400과 403을 구분해 Endpoint의 Query 규칙을 알아낼 수 있습니다.</p>
     */
    @Test
    void rejectsNonOwnerEvenWhenPageBoundsAreAlsoInvalid() {
        AuthPrincipal worker = new AuthPrincipal(9L, UserRole.WORKER, "김근로");

        assertThrows(RoleMismatchException.class, () -> service.findOwnedWorkplaces(worker, -1, 101));
        verifyNoInteractions(workplaceMapper);
    }

    @Test
    void rejectsPageBoundaryViolationBeforeQueryingList() {
        assertThrows(ValidationException.class, () -> service.findOwnedWorkplaces(owner(7L), 0, 101));
        assertThrows(ValidationException.class, () -> service.findOwnedWorkplaces(owner(7L), 0, 0));
        assertThrows(ValidationException.class, () -> service.findOwnedWorkplaces(owner(7L), -1, 20));

        verify(workplaceMapper, never()).findPageByOwnerUserId(anyLong(), anyInt(), anyLong());
        verify(workplaceMapper, never()).countByOwnerUserId(anyLong());
    }

    /** 첫 등록 전 OWNER는 오류가 아니라 빈 Page입니다. */
    @Test
    void returnsEmptyPageForOwnerWithoutWorkplaces() {
        when(workplaceMapper.countByOwnerUserId(7L)).thenReturn(0);
        when(workplaceMapper.findPageByOwnerUserId(7L, 20, 0L)).thenReturn(List.of());

        PageResponse<WorkplaceListItemResponse> response = service.findOwnedWorkplaces(owner(7L), 0, 20);

        assertTrue(response.getContent().isEmpty());
        assertEquals(0L, response.getPage().getTotalElements());
        assertEquals(0, response.getPage().getTotalPages());
    }

    private WorkplaceListRow row(Long workplaceId, String status) {
        return WorkplaceListRow.builder()
                .workplaceId(workplaceId)
                .businessRegistrationNumber("1234567890")
                .name("강남점")
                .representativeName("김사장")
                .roadAddress("서울 강남구 테헤란로 1")
                .detailAddress("2층")
                .phone("0212345678")
                .radiusMeters(new BigDecimal("100.00"))
                .attendanceLocationConfirmed(true)
                .status(status)
                .build();
    }

    private WorkplaceCoordinateConfirmCommand coordinateCommand(
            BigDecimal latitude,
            BigDecimal longitude,
            Instant capturedAt) {
        return WorkplaceCoordinateConfirmCommand.builder()
                .workplaceId(11L)
                .latitude(latitude)
                .longitude(longitude)
                .accuracyMeters(new BigDecimal("10.25"))
                .capturedAt(capturedAt)
                .build();
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
