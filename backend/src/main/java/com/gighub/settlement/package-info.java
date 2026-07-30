/**
 * 근무 완료 후 정산, 환불과 노쇼 처리 흐름을 담당합니다.
 *
 * <p>수동 승인은 SettlementService가 정산 상태와 지급을 한 Transaction으로 처리합니다.
 * 자동 정산 Scheduler와 환불·노쇼 정책은 같은 잠금 순서와 상태 계약을 재사용하도록 후속 구현합니다.</p>
 */
package com.gighub.settlement;

