package com.gighub.attendance.mapper.param;

import lombok.Getter;
import lombok.Setter;

/**
 * 활성 고정 QR 한 건의 INSERT 입력입니다.
 *
 * <p>{@code status}와 {@code created_at}은 호출자가 정하지 않습니다. Mapper XML이 계약값을
 * 직접 적습니다.</p>
 */
@Getter
@Setter
public class QrTokenInsertParam {

    /** {@code useGeneratedKeys}로 채워지는 생성 식별자입니다. */
    private Long id;

    private Long workplaceId;
    private Long issuedByUserId;
    private byte[] tokenNonce;
}
