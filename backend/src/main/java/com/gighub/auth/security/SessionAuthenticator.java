package com.gighub.auth.security;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

/**
 * 폼 로그인 필터 없이 컨트롤러에서 직접 로그인을 처리하므로, 세션 고정 방지(session id
 * 회전)와 CSRF 토큰 회전을 Spring Security의 기본 인증 흐름 대신 여기서 수동으로 한다.
 */
@Component
public class SessionAuthenticator {

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final CsrfTokenRepository csrfTokenRepository;

    public SessionAuthenticator(CsrfTokenRepository csrfTokenRepository) {
        this.csrfTokenRepository = csrfTokenRepository;
    }

    public void login(HttpServletRequest request, HttpServletResponse response, AuthPrincipal principal) {
        request.getSession(true);
        request.changeSessionId();

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.getRole()));
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        csrfTokenRepository.saveToken(csrfTokenRepository.generateToken(request), request, response);
    }
}
