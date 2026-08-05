package com.gighub.workplace.mapper.param;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 사업장 INSERT 파라미터입니다.
 *
 * <p>{@code ownerUserId}는 요청 Body가 아니라 인증 Principal에서 채웁니다. 인증 반경과
 * 최초 상태는 사용자도 서버 호출자도 정할 수 없는 계약값이라 필드로 두지 않고 Mapper XML이
 * 직접 기록합니다.</p>
 *
 * <p>{@code id}만 MyBatis가 생성 Key를 되돌려 쓰기 위해 가변이고 나머지 입력값은 Builder로만
 * 정합니다.</p>
 */
@Getter
@Builder
public class WorkplaceInsertParam {

    @Setter
    private Long id;

    private final Long ownerUserId;
    private final String businessRegistrationNumber;
    private final String name;
    private final String representativeName;
    private final String roadAddress;
    private final String detailAddress;
    private final String phone;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
}
