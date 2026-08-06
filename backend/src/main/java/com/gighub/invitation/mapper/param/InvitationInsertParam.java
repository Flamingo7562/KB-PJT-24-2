package com.gighub.invitation.mapper.param;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 초대 INSERT 파라미터입니다.
 *
 * <p>{@code tokenHash}가 없는 것이 의도된 설계입니다. Token 원문은 저장된 초대 ID에서
 * 파생하므로 INSERT 시점에는 아직 ID가 없어 Hash를 만들 수 없습니다. Mapper가 임시 Hash로
 * 행을 만들어 ID를 확보한 뒤 같은 Transaction에서 실제 Hash로 갱신합니다.</p>
 *
 * <p>{@code expectedTermsVersion}과 {@code expiresAt}은 잠근 Work Case에서 복사한 값이며
 * client가 지정할 수 없습니다.</p>
 */
@Getter
@Builder
public class InvitationInsertParam {

    /** MyBatis가 생성 Key를 되돌려 쓰기 위해 이 필드만 가변입니다. */
    @Setter
    private Long id;

    private final Long workCaseId;
    private final Integer expectedTermsVersion;
    private final LocalDateTime expiresAt;
}
