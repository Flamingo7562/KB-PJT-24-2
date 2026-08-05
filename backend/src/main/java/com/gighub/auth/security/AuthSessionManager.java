package com.gighub.auth.security;

import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

/** 수동 인증 API와 Spring Security의 HttpSession 수명주기를 연결합니다. */
@Component
public class AuthSessionManager {

    private final CsrfTokenRepository csrfTokenRepository;
    private final HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthSessionManager(CsrfTokenRepository csrfTokenRepository) {
        this.csrfTokenRepository = csrfTokenRepository;
    }

    public void establish(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthPrincipal principal) {
        if (request.getSession(false) == null) {
            request.getSession(true);
        } else {
            // 기존 익명 Session이 있을 때만 Servlet API로 ID를 회전합니다.
            request.changeSessionId();
        }

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(
                "ROLE_" + principal.getRole().name()
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(authority)
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        // 인증 경계가 바뀌었으므로 기존 Token을 폐기하고 클라이언트가 새 Token을 준비하게 합니다.
        csrfTokenRepository.saveToken(null, request, response);
    }

    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {
        csrfTokenRepository.saveToken(null, request, response);
        new SecurityContextLogoutHandler().logout(request, response, authentication);

        // Session 무효화만으로는 만료 Cookie가 내려가지 않으므로 명시적으로 제거합니다.
        Cookie sessionCookie = new Cookie("JSESSIONID", "");
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge(0);
        sessionCookie.setHttpOnly(true);
        sessionCookie.setSecure(request.isSecure());
        response.addCookie(sessionCookie);
    }
}
