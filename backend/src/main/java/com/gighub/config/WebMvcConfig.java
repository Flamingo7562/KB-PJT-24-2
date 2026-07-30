package com.gighub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.gighub.auth.exception.AuthExceptionHandler;

/**
 * DispatcherServlet이 사용하는 Spring MVC 전용 설정입니다.
 *
 * <p>{@link Controller} 계열 Bean만 검색하여 Root Context와 책임을 분리합니다. Jackson이
 * classpath에 있으므로 기본 JSON MessageConverter는 Spring MVC가 등록합니다.</p>
 *
 * <p>{@code @RestControllerAdvice}는 컴포넌트 스캔 대상이 아니라서(Controller 계열이
 * 아님) 필요한 것만 이 클래스에서 명시적으로 Bean 등록한다 — Root Context에 두면
 * {@code ExceptionHandlerExceptionResolver}가 찾지 못한다(SecurityConfig에서 겪은
 * Root/Servlet Context 분리 문제와 같은 원인).</p>
 *
 * <p>TODO: API 오류 규격이 정해지면 Validator, ArgumentResolver와 공통 응답 설정을 추가합니다.
 * 배포 Origin이 확정되기 전에는 전역 CORS 허용 규칙을 추가하지 않습니다.</p>
 */
@Configuration
@EnableWebMvc
@Import(SwaggerConfig.class)
@ComponentScan(
        basePackages = "com.gighub",
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ANNOTATION,
                classes = Controller.class
        ),
        useDefaultFilters = false
)
public class WebMvcConfig implements WebMvcConfigurer {

    @Bean
    public AuthExceptionHandler authExceptionHandler() {
        return new AuthExceptionHandler();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Swagger UI 화면 정적 리소스 매핑 허용
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/");

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}

