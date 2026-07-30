package com.gighub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Configuration
@EnableOpenApi
public class SwaggerConfig {
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.OAS_30)
                .ignoredParameterTypes(HttpSession.class, HttpServletRequest.class, HttpServletResponse.class)
                .useDefaultResponseMessages(false) // 기본 응답 메시지(200, 401 등) 자동 추가 끄기
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.gighub")) // API 컨트롤러가 있는 최상위 패키지
                .paths(PathSelectors.ant("/api/**")) // /api/ 로 시작하는 주소만 문서화
                .build()
                .pathMapping("/")
                .apiInfo(apiInfo());
    }

    private ApiInfo apiInfo(){
        return new ApiInfoBuilder()
                .title("GigHub API 명세서")
                .description("GigHub API 문서입니다.")
                .version("1.0.0")
                .build();
    }
}
