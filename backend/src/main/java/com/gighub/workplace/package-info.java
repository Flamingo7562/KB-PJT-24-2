/**
 * OWNER 사업장 등록과 소유 사업장 조회를 담당합니다.
 *
 * <p>소유자는 요청 Body가 아니라 인증 Principal에서만 결정하며, 인증 반경은 사용자 입력을
 * 받지 않고 계약값 100m를 서버가 적용합니다.</p>
 */
package com.gighub.workplace;
