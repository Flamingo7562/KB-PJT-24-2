/**
 * 여러 Operation이 공유하는 멱등 요청 Claim을 관리합니다.
 *
 * <p>같은 {@code Idempotency-Key}로 들어온 재전송이 자금이나 상태를 두 번 반영하지 않도록,
 * 최초 성공의 응답을 저장해 두고 그대로 다시 돌려줍니다. 저장 범위는
 * {@code (인증 사용자, Operation, Key)}이며 요청 내용이 같은지는 Fingerprint로 판정합니다.</p>
 *
 * <p>이 패키지는 특정 도메인에 속하지 않습니다. 충전·출금·초대 수락·정산 승인이 각자
 * Fingerprint 규칙만 정하고 생명주기는 여기서 공유합니다.</p>
 */
package com.gighub.idempotency;
