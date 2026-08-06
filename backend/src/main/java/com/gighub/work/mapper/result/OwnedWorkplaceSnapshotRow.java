package com.gighub.work.mapper.result;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 근무 등록 시점에 복사할 사업장 Snapshot 원본입니다.
 *
 * <p>조회 SQL에 소유권과 {@code ACTIVE} 조건이 함께 들어 있어, 결과가 있다는 것 자체가 인증
 * OWNER가 소유한 활성 사업장이라는 뜻입니다. 소유권을 별도 조회로 확인하면 확인과 사용
 * 사이에 상태가 바뀔 틈이 생깁니다.</p>
 *
 * <p>{@code radiusMeters}를 상수 100 대신 사업장 값에서 읽습니다. 저장되는 Snapshot이 실제
 * 사업장 설정과 항상 같아야 근태 검증이 어긋나지 않습니다.</p>
 *
 * <p>MyBatis가 &lt;constructor&gt; 매핑으로 생성하므로 no-args 생성자 없이 필드를 final로
 * 고정합니다. 필드 선언 순서가 곧 생성자 인자 순서이므로 XML과 함께 바꿔야 합니다.</p>
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class OwnedWorkplaceSnapshotRow {

    private final Long workplaceId;
    private final String workplaceName;
    private final String roadAddress;
    private final String detailAddress;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final BigDecimal radiusMeters;
}
