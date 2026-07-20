package com.gighub.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * DispatcherServlet이 사용하는 Spring MVC 전용 설정입니다.
 *
 * <p>{@link Controller} 계열 Bean만 검색하여 Root Context와 책임을 분리합니다. Jackson이
 * classpath에 있으므로 기본 JSON MessageConverter는 Spring MVC가 등록합니다.</p>
 *
 * <p>TODO: API 오류 규격이 정해지면 Validator, ArgumentResolver와 공통 응답 설정을 추가합니다.
 * 배포 Origin이 확정되기 전에는 전역 CORS 허용 규칙을 추가하지 않습니다.</p>
 */
@Configuration
@EnableWebMvc
@ComponentScan(
        basePackages = "com.gighub",
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ANNOTATION,
                classes = Controller.class
        ),
        useDefaultFilters = false
)
public class WebMvcConfig implements WebMvcConfigurer {
    // 기본 MVC 설정으로 시작하고 실제 요구가 생길 때 필요한 메서드만 재정의합니다.
}

