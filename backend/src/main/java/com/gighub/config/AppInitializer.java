package com.gighub.config;

import java.nio.charset.StandardCharsets;

import javax.servlet.Filter;

import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

/**
 * {@code web.xml}을 대신해 Spring Root Context와 DispatcherServlet을 초기화합니다.
 *
 * <p>Tomcat 9가 Servlet 4.0 애플리케이션을 시작할 때 이 클래스를 자동으로 발견합니다. 모든 요청은
 * DispatcherServlet의 {@code /} 매핑을 거치며, 요청과 응답은 UTF-8로 통일합니다.</p>
 *
 * <p>TODO: 파일 업로드 정책이 확정되면 허용 크기와 임시 저장 경로를 명시한 Multipart 설정을
 * 추가합니다.</p>
 */
public class AppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    /**
     * Service와 영속성 Bean이 들어갈 Root Context 설정을 반환합니다.
     *
     * @return Root Context 설정 클래스 목록
     */
    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[]{RootConfig.class};
    }

    /**
     * Controller와 Spring MVC Bean이 들어갈 Servlet Context 설정을 반환합니다.
     *
     * @return Servlet Context 설정 클래스 목록
     */
    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[]{WebMvcConfig.class};
    }

    /**
     * DispatcherServlet이 처리할 기본 URL 범위를 지정합니다.
     *
     * @return Servlet 매핑 목록
     */
    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }

    /**
     * 한글 요청 본문과 JSON 응답의 인코딩을 UTF-8로 강제합니다.
     *
     * @return DispatcherServlet 앞에서 실행할 Filter 목록
     */
    @Override
    protected Filter[] getServletFilters() {
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding(StandardCharsets.UTF_8.name());
        encodingFilter.setForceEncoding(true);
        return new Filter[]{encodingFilter};
    }
}

