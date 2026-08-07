package com.gighub.workplace.dto;

import com.gighub.workplace.mapper.result.WorkplaceListRow;
import lombok.Getter;

/**
 * OWNER 사업장 목록의 Item 하나입니다.
 *
 * <p>승인 명세가 고정한 아홉 필드만 두고, 목록에서 제외된 {@code latitude}·{@code longitude}는
 * 조회 행에도 응답에도 없습니다.</p>
 */
@Getter
public final class WorkplaceListItemResponse {

    private final Long workplaceId;
    private final String businessRegistrationNumber;
    private final String name;
    private final String representativeName;
    private final String roadAddress;
    private final String detailAddress;
    private final String phone;
    private final int radiusMeters;
    private final boolean attendanceLocationConfirmed;
    private final String status;

    private WorkplaceListItemResponse(WorkplaceListRow row) {
        this.workplaceId = row.getWorkplaceId();
        this.businessRegistrationNumber = row.getBusinessRegistrationNumber();
        this.name = row.getName();
        this.representativeName = row.getRepresentativeName();
        this.roadAddress = row.getRoadAddress();
        this.detailAddress = row.getDetailAddress();
        this.phone = row.getPhone();
        // 명세의 반경은 정수 100입니다. DECIMAL(8,2)를 그대로 직렬화하면 100.00이 나가므로
        // 저장 정밀도를 응답 계약으로 흘리지 않고 여기서 정수로 맞춥니다.
        this.radiusMeters = row.getRadiusMeters().intValue();
        this.attendanceLocationConfirmed = row.isAttendanceLocationConfirmed();
        this.status = row.getStatus();
    }

    public static WorkplaceListItemResponse from(WorkplaceListRow row) {
        return new WorkplaceListItemResponse(row);
    }
}
