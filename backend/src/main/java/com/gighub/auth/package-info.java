/**
 * 로그인, 로그아웃, 서버 Session과 권한 검사를 담당합니다.
 *
 * <p>{@code security} 하위 패키지에 세션 인증·CSRF·JSON 401/403 처리
 * ({@link com.gighub.auth.security.SecurityConfig})가 구현되어 있습니다.
 * 회원가입·로그인·세션조회·로그아웃 API 자체(컨트롤러·서비스·DTO)는 이슈 #71의
 * 후속 브랜치에서 추가합니다. JWT는 인증 방식에 포함하지 않습니다.</p>
 */
package com.gighub.auth;

